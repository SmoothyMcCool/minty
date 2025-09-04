package tom.ollama.service;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

public interface OllamaService {

	String getDefaultModel();

	EmbeddingModel getEmbeddingModel();

	VectorStore getVectorStore();

	ChatMemoryRepository getChatMemoryRepository();

	ChatMemory getChatMemory();

	List<String> listModels();

}
