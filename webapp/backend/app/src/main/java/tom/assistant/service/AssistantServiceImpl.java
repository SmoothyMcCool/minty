package tom.assistant.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import tom.assistant.repository.AssistantRepository;
import tom.task.model.Assistant;
import tom.task.model.AssistantQuery;
import tom.task.model.AssistantState;
import tom.task.services.AssistantService;
import tom.task.services.ConversationService;
import tom.task.services.DocumentService;

@Service
public class AssistantServiceImpl implements AssistantService {

	private static final Logger logger = LogManager.getLogger(AssistantServiceImpl.class);

	private final AssistantRepository assistantRepository;
	private final DocumentService documentService;
	private final ConversationService conversationService;
	private final VectorStore vectorStore;
	private final ChatMemory chatMemory;
	private final OllamaApi ollamaApi;

	private static final int MaxResults = 5;

	public AssistantServiceImpl(AssistantRepository assistantRepository, DocumentService documentService,
			ConversationService conversationService, VectorStore vectorStore, ChatMemory chatMemory,
			OllamaApi ollamaApi) {

		this.assistantRepository = assistantRepository;
		this.documentService = documentService;
		this.conversationService = conversationService;
		this.vectorStore = vectorStore;
		this.chatMemory = chatMemory;
		this.ollamaApi = ollamaApi;
	}

	@Override
	public Assistant createAssistant(int userId, Assistant assistant) {
		tom.assistant.repository.Assistant repoAsst = new tom.assistant.repository.Assistant(assistant);
		repoAsst.setOwnerId(userId);
		repoAsst.setId(null);
		if (repoAsst.getNumFiles() > 0) {
			repoAsst.setState(AssistantState.PROCESSING_FILES);
		} else {
			repoAsst.setState(AssistantState.READY);
		}
		repoAsst.setProcessedFiles(0);

		return assistantRepository.save(repoAsst).toTaskAssistant();
	}

	@Override
	public List<Assistant> listAssistants(int userId) {
		List<tom.assistant.repository.Assistant> asstList = assistantRepository.findAllByOwnerIdOrSharedTrue(userId);

		if (asstList == null || asstList.size() == 0) {
			return new ArrayList<>();
		}

		return asstList.stream().map(asst -> asst.toTaskAssistant()).toList();
	}

	@Override
	public Assistant findAssistant(int userId, int assistantId) {
		tom.assistant.repository.Assistant assistant = assistantRepository.findById(assistantId).get();
		if (assistant.isShared() || assistant.getOwnerId() == userId) {
			return assistant.toTaskAssistant();
		}
		return null;
	}

	@Override
	public boolean deleteAssistant(int userId, int assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);

		if (assistant == null) {
			return false;
		}

		assistantRepository.deleteById(assistantId);
		documentService.deleteDocumentsForAssistant(assistantId);
		conversationService.deleteConversationsForAssistant(assistantId);

		return true;

	}

	@Override
	public String ask(int userId, AssistantQuery query) {
		var chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(OllamaOptions.builder().model(OllamaModel.LLAMA3_2).temperature(0.9).build()).build();

		List<Advisor> advisors = new ArrayList<>();
		advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());

		boolean defaultAssistant = false;
		String conversationId = null;
		String prompt = null;

		if (query.getConversationId() == null || query.getConversationId().compareTo("default") == 0) {
			defaultAssistant = true;
			conversationId = conversationService.getDefaultConversationId(userId);
		} else {
			conversationId = query.getConversationId();
			Assistant assistant = findAssistant(userId, query.getAssistantId());
			assistantRepository.findById(query.getAssistantId()).get();

			if (assistant == null) {
				logger.warn(
						"Tried to build a chat session from an assistant " + "that user doesn't have access to. User: "
								+ userId + ", assistant: " + query.getAssistantId());
				return null;
			}

			prompt = assistant.prompt();

			if (assistant.numFiles() > 0) {
				SearchRequest searchRequest = SearchRequest.builder().query(query.getQuery())
						.filterExpression(" assistantId == " + query.getAssistantId()).topK(MaxResults).build();
				Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
				advisors.add(ragAdvisor);
			}
		}

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisors).build();

		ChatClientRequestSpec spec = null;
		if (defaultAssistant || prompt == null) {
			spec = chatClient.prompt();
		} else {
			spec = chatClient.prompt(prompt);
		}

		final String finalConversationId = conversationId;
		ChatResponse chatResponse = spec.user(query.getQuery())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId)).call().chatResponse();

		if (chatResponse != null) {
			return chatResponse.getResult().getOutput().getText();
		}

		logger.warn("ask: Chat response was null");
		return null;
	}

}
