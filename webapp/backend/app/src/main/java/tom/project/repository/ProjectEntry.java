package tom.project.repository;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.ProjectEntryId;
import tom.api.model.project.ProjectEntryInfo;
import tom.api.model.project.ProjectEntryType;

@Entity
public class ProjectEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UUID projectId;
	@Enumerated(EnumType.STRING)
	private ProjectEntryType type;
	private String name;
	private UUID parentId;
	private String data;

	public ProjectEntry() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public void setProjectId(UUID projectId) {
		this.projectId = projectId;
	}

	public ProjectEntryType getType() {
		return type;
	}

	public void setType(ProjectEntryType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getParentId() {
		return parentId;
	}

	public void setParentId(UUID parentId) {
		this.parentId = parentId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public tom.api.model.project.ProjectEntry toModel() {
		ProjectEntryInfo info = new ProjectEntryInfo();
		info.setId(new ProjectEntryId(getId()));
		info.setName(getName());
		info.setType(getType());
		return new tom.api.model.project.ProjectEntry(info, getData());
	}

}
