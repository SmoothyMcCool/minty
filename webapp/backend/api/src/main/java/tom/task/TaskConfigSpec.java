package tom.task;

import java.util.List;
import java.util.Map;

public interface TaskConfigSpec {

	Map<String, String> getValues();

	Map<String, TaskConfigTypes> getConfig();

	List<String> getSystemConfigVariables();

	List<String> getUserConfigVariables();
}
