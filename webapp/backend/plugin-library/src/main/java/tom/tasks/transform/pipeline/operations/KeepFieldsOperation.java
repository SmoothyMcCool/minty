package tom.tasks.transform.pipeline.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class KeepFieldsOperation implements TransformOperation {

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		if (!config.isList()) {
			throw new IllegalArgumentException("KeepFieldsOperation requires a list configuration");
		}

		List<Object> list = config.getList().get();

		Set<String> allowedFields = new HashSet<>();
		for (Object o : list) {
			allowedFields.add(String.valueOf(o));
		}

		for (Map<String, Object> row : packet.getData()) {
			row.keySet().removeIf(key -> !allowedFields.contains(key));
		}
	}

	@Override
	public String getName() {
		return "Keep Fields";
	}

}
