package tom.ollama.service;

import java.util.Objects;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

public record ModelObject(EmbeddingModel embeddingModel, VectorStore vectorStore, ChatMemory chatMemory,
		ChatMemoryRepository chatMemoryRepository) {

	public ModelObject {
		Objects.requireNonNull(embeddingModel, "embeddingModel cannot be null");
		Objects.requireNonNull(vectorStore, "vectorStore cannot be null");
		Objects.requireNonNull(chatMemory, "chatMemory cannot be null");
		Objects.requireNonNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
	}

}
