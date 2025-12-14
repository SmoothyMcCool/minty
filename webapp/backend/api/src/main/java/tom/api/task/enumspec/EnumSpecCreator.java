package tom.api.task.enumspec;

import tom.api.UserId;
import tom.api.services.TaskServices;

public interface EnumSpecCreator {

	EnumSpec getEnumList(UserId userId);

	String getName();

	default void setTaskServices(TaskServices taskServices) {

	}

}
