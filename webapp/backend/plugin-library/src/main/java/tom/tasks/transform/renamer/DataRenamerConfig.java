package tom.tasks.transform.renamer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class DataRenamerConfig implements TaskConfigSpec {

	private final String InputRenames = "Input Renamings";

	Map<String, String> renames;

	public DataRenamerConfig() {
		renames = Map.of();
	}

	public DataRenamerConfig(Map<String, String> config) {
		this();
		if (config.containsKey(InputRenames)) {
			String renameObj = config.get(InputRenames);

			if (renameObj == null || renameObj.isBlank()) {
				return;
			}

			renames = Arrays.stream(renameObj.split(",")).map(s -> s.split(":"))
					.collect(Collectors.toMap(array -> array[0].trim(), array -> array[1].trim()));

		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(InputRenames, TaskConfigTypes.Map);
		return config;
	}

	Map<String, String> getRenames() {
		return renames;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	@Override
	public Map<String, String> getValues() {
		return Map.of(InputRenames, renames.entrySet().stream().map(e -> e.getKey().trim() + ":" + e.getValue().trim())
				.collect(Collectors.joining(",")));
	}
}
