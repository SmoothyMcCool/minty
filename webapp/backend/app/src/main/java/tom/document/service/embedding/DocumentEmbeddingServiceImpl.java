package tom.document.service.embedding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import tom.api.DocumentId;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.services.DocumentExtractorService;
import tom.config.MintyConfiguration;
import tom.document.service.DocumentEmbeddingService;
import tom.document.service.DocumentProcessingException;
import tom.llm.service.LlmService;

@Service
public class DocumentEmbeddingServiceImpl implements DocumentEmbeddingService {

	private static final Logger logger = LogManager.getLogger(DocumentEmbeddingServiceImpl.class);

	private final DocumentExtractorService documentExtractorService;
	private final LlmService llmService;

	private final int keywordsPerDocument;
	private final int documentTargetChunkSize;
	private final int macroTargetChunkSize;
	private final int embeddingBatchSize;
	// private final int maxEmbeddingTokens;

	private final ChatModel chatModel;

	public DocumentEmbeddingServiceImpl(DocumentExtractorService documentExtractorService, LlmService llmService,
			MintyConfiguration properties) {
		this.documentExtractorService = documentExtractorService;
		this.llmService = llmService;
		keywordsPerDocument = properties.getConfig().llm().embedding().keywordsPerDocument();
		documentTargetChunkSize = properties.getConfig().llm().embedding().documentTargetChunkSize();
		macroTargetChunkSize = properties.getConfig().llm().embedding().macroTargetChunkSize();
		embeddingBatchSize = properties.getConfig().llm().embedding().batchSize();

		// maxEmbeddingTokens =
		// properties.getConfig().llm().embedding().maxEmbeddingTokens();

		String summarizingModel = properties.getConfig().llm().embedding().summarizingModel();
		chatModel = llmService.buildSimpleModel(summarizingModel);

	}

	@Override
	public void embed(Document doc) {
		List<DocumentSection> sections = doc.sections();
		List<org.springframework.ai.document.Document> rawDocuments = sections.stream()
				.filter(s -> !s.content().isBlank()).map(s -> {
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("section_title", s.title());
					metadata.put("section_level", s.level());
					metadata.put("section_breadcrumb", documentExtractorService.buildBreadcrumb(sections, s));
					return new org.springframework.ai.document.Document(s.content(), metadata);
				}).toList();

		// Summarize larger sections first
		List<org.springframework.ai.document.Document> macroChunks = split(rawDocuments, macroTargetChunkSize);
		macroChunks = normalize(macroChunks);
		macroChunks = summarize(macroChunks, chatModel);
		macroChunks = enrich(doc.id(), macroChunks, chatModel);

		// Now split into small chunks for embedding
		List<org.springframework.ai.document.Document> microChunks = split(macroChunks, documentTargetChunkSize);
		for (org.springframework.ai.document.Document document : microChunks) {
			document.getMetadata().put("documentId", doc.id());
			document.getMetadata().put("source", doc.title());
		}

		for (int i = 0; i < microChunks.size(); i += embeddingBatchSize) {
			List<org.springframework.ai.document.Document> batch = microChunks.subList(i,
					Math.min(i + embeddingBatchSize, microChunks.size()));
			if (!addDocuments(batch, documentTargetChunkSize, 0)) {
				String batchStr = batch.stream().map(d -> d.getText()).collect(Collectors.joining("\n-----\n"));
				throw new DocumentProcessingException(
						"Failed to store document despite repeated re-chunking. Failing. The failing chunk was: "
								+ batchStr);
			}
		}
	}

	private List<org.springframework.ai.document.Document> split(
			List<org.springframework.ai.document.Document> documents, int targetChunkSize) {
		TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(targetChunkSize).build();
		return splitter.apply(documents);
	}

	private List<org.springframework.ai.document.Document> normalize(
			List<org.springframework.ai.document.Document> documents) {
		return documents.stream().map(doc -> {
			String text = doc.getText();

			if (StringUtils.isBlank(text)) {
				return null;
			}

			String clean = text.replaceAll("\\s+", " ").replaceAll("data:image/[^;]+;base64,[A-Za-z0-9+/=]+", "[B64]")
					.replaceAll("\\{[^}]{1000,}\\}", "[JSON_BLOCK]");

			return new org.springframework.ai.document.Document(clean, doc.getMetadata());
		}).filter(Objects::nonNull).toList();

	}

	private List<org.springframework.ai.document.Document> enrich(DocumentId documentId,
			List<org.springframework.ai.document.Document> documents, ChatModel chatModel) {
		KeywordMetadataEnricher keywordifier = new KeywordMetadataEnricher(chatModel, keywordsPerDocument);
		List<org.springframework.ai.document.Document> keywordedDocuments = keywordifier.apply(documents);

		return keywordedDocuments;
	}

	private List<org.springframework.ai.document.Document> summarize(
			List<org.springframework.ai.document.Document> documents, ChatModel chatModel) {
		SummaryMetadataEnricher summarizer = new SummaryMetadataEnricher(chatModel, List.of(SummaryType.CURRENT));
		// List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
		return summarizer.apply(documents);
	}

	private boolean addDocuments(List<org.springframework.ai.document.Document> documents, int targetChunkSize,
			int iteration) {

		if (iteration >= 5) {
			return false;
		}

		VectorStore vectorStore = llmService.getVectorStore();
		try {
			vectorStore.add(documents);
			return true;
		} catch (Exception e) {
			logger.warn("Chunk too large. Splitting.");
			// split again and retry
			int newTarget = targetChunkSize / 2;
			return addDocuments(split(documents, newTarget), newTarget, iteration + 1);
		}
	}
}
