package tom.tools.toolregistry;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;

import tom.tools.model.MintyToolDescription;

public interface ToolRegistryService {

	ToolCallback getTool(String toolName);

	List<MintyToolDescription> listTools();

}
