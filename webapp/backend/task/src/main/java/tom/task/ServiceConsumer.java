package tom.task;

import tom.task.services.TaskServices;

public interface ServiceConsumer {

    void setTaskServices(TaskServices taskServices);

    // Optional method, if userId is not required.
    default void setUserId(int userId) {
    }
}
