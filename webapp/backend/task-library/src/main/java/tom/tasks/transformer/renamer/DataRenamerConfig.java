package tom.tasks.transformer.renamer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class DataRenamerConfig implements TaskConfig {

	Map<String, String> renames = Map.of();

	public DataRenamerConfig() {
	}

	public DataRenamerConfig(Map<String, String> config) {
		String renameObj = config.get("Input Renamings");

		if (renameObj == null || renameObj.isBlank()) {
			return;
		}

		renames = Arrays.stream(renameObj.split(",")).map(s -> s.split(":"))
				.collect(Collectors.toMap(array -> array[0].trim(), array -> array[1].trim()));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Input Renamings", TaskConfigTypes.Map);
		return config;
	}

	Map<String, String> getRenames() {
		return renames;
	}
}
