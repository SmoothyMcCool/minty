package tom.tasks.transformer.renamer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Rename Fields", configClass = "tom.tasks.transformer.renamer.DataRenamerConfig")
public class DataRenamer implements AiTask {

	private UUID uuid = UUID.randomUUID();
	private DataRenamerConfig config;
	Map<String, String> input;

	public DataRenamer() {

	}

	public DataRenamer(DataRenamerConfig config) {
		this.config = config;
	}

	@Override
	public String taskName() {
		return "RenameFields-" + uuid.toString();
	}

	@Override
	public Map<String, Object> getResult() {
		return Map.of();
	}

	@Override
	public List<Map<String, String>> runTask() {
		Map<String, String> result = new HashMap<>();
		Map<String, String> renames = config.getRenames();

		if (input == null) {
			return List.of();
		}

		input.forEach((key, value) -> {
			if (renames.containsKey(key)) {
				result.put(renames.get(key), value);
			} else {
				result.put(key, value);
			}
		});

		return List.of(result);
	}

	@Override
	public void setInput(Map<String, String> input) {
		this.input = input;
	}

	@Override
	public String expects() {
		return "This task accepts any input. It inspects all input keys and remaps them per the given configuration. "
				+ "For example, if the configuration is\n{ \"A\": \"Aa\", \"B\": \"Bb\" }\nand the input "
				+ "is\n{ \"A\": \"Antelope\",\"C\": \"Cantaloupe\" }\nthen the result will be\n"
				+ " { \\\"Aa\\\": \\\"Antelope\\\",\\\"C\\\": \\\"Cantaloupe\\\" }";
	}

	@Override
	public String produces() {
		return "The remapped input data.";
	}

}
