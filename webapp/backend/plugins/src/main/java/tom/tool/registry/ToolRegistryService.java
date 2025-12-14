package tom.tool.registry;

import java.util.List;

import tom.api.UserId;
import tom.tool.model.MintyToolDescription;

public interface ToolRegistryService {

	Object getTool(String toolName, UserId userId);

	List<MintyToolDescription> listTools();

	void loadTool(Class<?> loadedClass) throws ToolLoadFailureException;
}
