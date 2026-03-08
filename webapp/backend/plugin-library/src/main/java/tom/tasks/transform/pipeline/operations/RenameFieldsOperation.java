package tom.tasks.transform.pipeline.operations;

import java.util.HashMap;
import java.util.Map;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class RenameFieldsOperation implements TransformOperation {

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		Map<String, Object> renameMap = config.getMap()
				.orElseThrow(() -> new IllegalArgumentException("RenameFieldsOperation requires a map config"));

		for (Map<String, Object> row : packet.getData()) {

			Map<String, Object> updates = new HashMap<>();

			for (Map.Entry<String, Object> entry : renameMap.entrySet()) {

				String oldKey = entry.getKey();
				String newKey = String.valueOf(entry.getValue());

				if (row.containsKey(oldKey)) {
					updates.put(newKey, row.get(oldKey));
					row.remove(oldKey);
				}
			}
		}
	}

	@Override
	public String getName() {
		return "Rename Fields";
	}
}
