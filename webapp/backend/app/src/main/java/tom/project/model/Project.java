package tom.project.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tom.api.ProjectId;
import tom.api.UserId;

@Entity
@Table(name = "Project")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UserId ownerId;

	private String name;

	@Column(nullable = false)
	private Instant created;

	@Column(nullable = false)
	private Instant updated;

	public ProjectId getId() {
		return new ProjectId(id);
	}

	public void setId(ProjectId id) {
		this.id = id == null ? null : id.value();
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

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

	public Instant getUpdated() {
		return updated;
	}

	public void setUpdated(Instant updated) {
		this.updated = updated;
	}

	public tom.api.model.project.Project toModel() {
		return new tom.api.model.project.Project(new ProjectId(id), name);
	}

}
