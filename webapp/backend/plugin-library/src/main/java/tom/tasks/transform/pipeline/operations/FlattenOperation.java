package tom.tasks.transform.pipeline.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public class FlattenOperation implements TransformOperation {

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		Map<String, Object> cfg = config.getMap()
				.orElseThrow(() -> new IllegalArgumentException("FlattenListOperation requires map config"));

		String precedence = String.valueOf(cfg.getOrDefault("precedence", "last"));
		String separator = String.valueOf(cfg.getOrDefault("separator", " "));

		boolean lastWins = !"first".equalsIgnoreCase(precedence);

		Map<String, Object> merged = new HashMap<>();

		for (Map<String, Object> row : packet.getData()) {

			for (Map.Entry<String, Object> entry : row.entrySet()) {

				if (lastWins) {
					merged.put(entry.getKey(), entry.getValue());
				} else {
					merged.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
		}

		packet.getData().clear();
		packet.addData(merged);

		StringJoiner joiner = new StringJoiner(separator);

		for (String text : packet.getText()) {
			if (text != null) {
				joiner.add(text);
			}
		}

		packet.getText().clear();
		packet.addText(joiner.toString());
	}

	@Override
	public String getName() {
		return "Flatten Lists";
	}
}
