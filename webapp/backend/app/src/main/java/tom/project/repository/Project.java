package tom.project.repository;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.ProjectId;
import tom.api.UserId;

@Entity
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UserId ownerId;
	private String name;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public tom.api.model.project.Project toModel() {
		return new tom.api.model.project.Project(new ProjectId(id), name);
	}

}
