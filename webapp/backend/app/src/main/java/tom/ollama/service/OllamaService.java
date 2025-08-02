package tom.ollama.service;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

public interface OllamaService {

	MintyOllamaModel getDefaultModel();

	EmbeddingModel getEmbeddingModel(MintyOllamaModel model);

	VectorStore getVectorStore(MintyOllamaModel model);

	ChatMemoryRepository getChatMemoryRepository(MintyOllamaModel model);

	ChatMemory getChatMemory(MintyOllamaModel model);

	List<MintyOllamaModel> listModels();

}
