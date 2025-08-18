package tom.tasks.transformer.emitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Emit Records", configClass = "tom.tasks.transformer.emitter.DataEmitterConfig")
public class DataEmitter implements AiTask {

	private UUID uuid = UUID.randomUUID();
	private DataEmitterConfig config;

	public DataEmitter() {

	}

	public DataEmitter(DataEmitterConfig config) {
		this.config = config;
	}

	@Override
	public String taskName() {
		return "EmitRecords-" + uuid.toString();
	}

	@Override
	public Map<String, Object> getResult() {
		return null;
	}

	@Override
	public List<Map<String, String>> runTask() {
		List<String> data = config.getData();
		String key = config.getKeyName();

		List<Map<String, String>> result = new ArrayList<>();

		data.forEach(datum -> {
			result.add(Map.of(key, datum));
		});

		return result;
	}

	@Override
	public void setInput(Map<String, String> input) {
	}

	@Override
	public String expects() {
		return "This task does nothing with any input it receives. It drops all input on the floor, in favour of the data that it is set up to emit.";
	}

	@Override
	public String produces() {
		return "An array of the items set in the configuration.";
	}

}
