package tom.task;

import java.util.List;
import java.util.Map;

public interface TaskConfig {

	Map<String, TaskConfigTypes> getConfig();

	List<String> getSystemConfigVariables();

	List<String> getUserConfigVariables();
}
