package tom.tasks.transform.pipeline.operations;

import java.util.Map;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class SetFieldOperation implements TransformOperation {

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		Map<String, Object> map = config.getMap()
				.orElseThrow(() -> new IllegalArgumentException("SetFieldOperation requires a map config"));

		String field = String.valueOf(map.get("field"));
		Object value = map.get("value");

		for (Map<String, Object> row : packet.getData()) {
			row.put(field, value);
		}
	}

	@Override
	public String getName() {
		return "Set Field";
	}

}
