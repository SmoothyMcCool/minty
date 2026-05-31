package tom.tool.registry;

import java.util.List;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.tool.MintyTool;
import tom.tool.model.MintyToolDescription;

public interface ToolRegistryService {

	MintyTool getTool(String toolName, UserId userId, ConversationId conversationId);

	MintyTool getProjectTools(UserId userId, ConversationId conversationId);

	List<MintyToolDescription> listTools();

	void loadTool(Class<?> loadedClass) throws ToolLoadFailureException;

}
