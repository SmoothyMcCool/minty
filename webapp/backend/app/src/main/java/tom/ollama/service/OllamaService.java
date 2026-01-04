package tom.ollama.service;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;

import tom.config.model.ChatModelConfig;

public interface OllamaService {

	String getDefaultModel();

	VectorStore getVectorStore();

	ChatMemoryRepository getChatMemoryRepository();

	ChatMemory getChatMemory();

	List<ChatModelConfig> listModels();

}
