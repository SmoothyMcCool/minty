package tom.llm.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.ChatModelConfig;

public interface LlmService {

	VectorStore getVectorStore();

	ChatMemoryRepository getChatMemoryRepository();

	ChatMemory getChatMemory();

	List<ChatModelConfig> listModels();

	boolean isModelValid(String model);

	ChatModel buildSimpleModel(String model);

	int estimateInputTokens(AssistantQuery query);

	ChatClient buildChatClient(Assistant assistant, AssistantQuery query, int contextSize, List<Advisor> advisors);

}
