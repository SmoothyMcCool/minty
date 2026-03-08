package tom.tasks.transform.pipeline.operations;

import java.util.Map;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class RemoveEmptyOperation implements TransformOperation {

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		// Clean text entries
		packet.getText().removeIf(t -> t == null || t.trim().isEmpty());

		// Clean data rows
		for (Map<String, Object> row : packet.getData()) {

			row.entrySet().removeIf(entry -> {

				Object value = entry.getValue();

				if (value == null) {
					return true;
				}

				if (value instanceof String) {
					return ((String) value).trim().isEmpty();
				}

				return false;
			});
		}
	}

	@Override
	public String getName() {
		return "Remove Empty Records";
	}
}
