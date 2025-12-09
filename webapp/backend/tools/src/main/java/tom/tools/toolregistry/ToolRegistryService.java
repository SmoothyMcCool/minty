package tom.tools.toolregistry;

import java.util.List;

import tom.api.UserId;
import tom.tools.model.MintyToolDescription;

public interface ToolRegistryService {

	Object getTool(String toolName, UserId userId);

	List<MintyToolDescription> listTools();

}
