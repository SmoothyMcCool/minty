package tom.tasks.transform.pipeline.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class RemoveFieldsOperation implements TransformOperation {

	public static final String OperationName = "Remove Fields";

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		List<Object> fields = config.getList().orElseThrow(
				() -> new IllegalArgumentException("RemoveFieldsOperation requires a list of field names"));

		// Convert to a set of strings for faster lookup
		Set<String> fieldNames = new HashSet<>();
		for (Object f : fields) {
			if (f != null) {
				fieldNames.add(String.valueOf(f));
			}
		}

		if (fieldNames.isEmpty()) {
			return;
		}

		for (Map<String, Object> row : packet.getData()) {
			if (row == null) {
				continue;
			}
			for (String field : fieldNames) {
				row.remove(field);
			}
		}
	}

	@Override
	public String getName() {
		return OperationName;
	}
}
