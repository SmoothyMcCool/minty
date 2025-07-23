package tom.ollama.service;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.vectorstore.VectorStore;

public interface OllamaService {

	OllamaModel getDefaultModel();

	EmbeddingModel getEmbeddingModel(OllamaModel model);

	VectorStore getVectorStore(OllamaModel model);

	ChatMemoryRepository getChatMemoryRepository(OllamaModel model);

	ChatMemory getChatMemory(OllamaModel model);

	List<OllamaModel> listModels();

}
