package tom.model;

import tom.api.UserId;
import tom.api.services.TaskServices;

public interface ServiceConsumer {

	void setTaskServices(TaskServices taskServices);

	// Optional method, if userId is not required.
	default void setUserId(UserId userId) {
	}
}
