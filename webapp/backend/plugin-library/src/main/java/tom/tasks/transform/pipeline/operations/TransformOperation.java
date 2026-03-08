package tom.tasks.transform.pipeline.operations;

import tom.api.task.Packet;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;

public interface TransformOperation {

	void execute(Packet packet, PipelineOperationConfiguration config);

	String getName();

}
