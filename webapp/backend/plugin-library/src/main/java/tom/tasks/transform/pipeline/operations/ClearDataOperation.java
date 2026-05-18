package tom.tasks.transform.pipeline.operations;

import tom.api.task.Packet;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

@RunnableTask
public class ClearDataOperation implements TransformOperation {

	public static final String OperationName = "Remove all Data";

	@Override
	public void execute(Packet packet, PipelineOperationConfiguration config) {
		packet.setData(null);
	}

	@Override
	public String getName() {
		return OperationName;
	}
}
