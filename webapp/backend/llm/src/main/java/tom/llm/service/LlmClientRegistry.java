package tom.llm.service;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.model.ChatModelConfig;
import tom.user.model.User;

public interface LlmClientRegistry {

	ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors);

	ChatModel buildSimpleModel(String modelName);

	VectorStore getVectorStore(String modelName);

	ChatMemoryRepository getChatMemoryRepository();

	ChatMemory getChatMemory();

	boolean has(String modelName);

	Set<String> modelNames();

	LlmEndpointService serviceForModel(String modelName);

	List<ChatModelConfig> listModels();

}
