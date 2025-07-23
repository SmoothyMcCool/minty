package tom.task.filesystem.repository;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.task.controller.TaskRequest;
import tom.task.converters.TaskRequestConverter;

@Entity
public class FilesystemWatcher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private String locationToWatch;
    @Convert(converter = TaskRequestConverter.class)
    private TaskRequest request;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocationToWatch() {
        return locationToWatch;
    }

    public void setLocationToWatch(String locationToWatch) {
        this.locationToWatch = locationToWatch;
    }

    public TaskRequest getRequest() {
        return request;
    }

    public void setRequest(TaskRequest request) {
        this.request = request;
    }

}
