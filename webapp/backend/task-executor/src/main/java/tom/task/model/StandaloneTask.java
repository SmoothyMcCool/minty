package tom.task.model;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.task.converters.TaskConverter;

@Entity
public class StandaloneTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private int ownerId;
	private boolean shared;
	private String name;
	private boolean triggered;
	private String watchLocation;
	@Convert(converter = TaskConverter.class)
	private Task taskTemplate;
	@Convert(converter = TaskConverter.class)
	private Task outputTemplate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isTriggered() {
		return triggered;
	}

	public void setTriggered(boolean triggered) {
		this.triggered = triggered;
	}

	public String getWatchLocation() {
		return watchLocation;
	}

	public void setWatchLocation(String watchLocation) {
		this.watchLocation = watchLocation;
	}

	public Task getTaskTemplate() {
		return taskTemplate;
	}

	public void setTaskTemplate(Task taskTemplate) {
		this.taskTemplate = taskTemplate;
	}

	public Task getOutputTemplate() {
		return outputTemplate;
	}

	public void setOutputTemplate(Task outputTemplate) {
		this.outputTemplate = outputTemplate;
	}

}
