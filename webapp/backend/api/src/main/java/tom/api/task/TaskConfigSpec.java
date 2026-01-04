package tom.api.task;

import java.util.List;
import java.util.Map;

public interface TaskConfigSpec {

	Map<String, Object> getValues();

	Map<String, TaskConfigTypes> getConfig();

	List<String> getSystemConfigVariables();

	List<String> getUserConfigVariables();
}
