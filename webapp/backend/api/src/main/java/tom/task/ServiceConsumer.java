package tom.task;

import java.util.UUID;

import tom.api.services.TaskServices;

public interface ServiceConsumer {

	void setTaskServices(TaskServices taskServices);

	// Optional method, if userId is not required.
	default void setUserId(UUID userId) {
	}
}
