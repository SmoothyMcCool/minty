package tom.tasks.transform.pipeline.operations;

import java.util.Map;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class RemoveNullsOperation implements TransformOperation {

	public static final String OperationName = "Remove Null Fields";

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		for (Map<String, Object> row : packet.getData()) {
			row.entrySet().removeIf(e -> e.getValue() == null);
		}

		packet.getText().removeIf(s -> s == null || s.isBlank());
	}

	@Override
	public String getName() {
		return OperationName;
	}
}
