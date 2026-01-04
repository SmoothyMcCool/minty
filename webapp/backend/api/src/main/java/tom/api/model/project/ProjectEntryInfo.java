package tom.api.model.project;

import tom.api.ProjectEntryId;

public class ProjectEntryInfo {
	private ProjectEntryId id;
	private ProjectEntryType type;
	private ProjectEntryId parent;
	private String name;

	public ProjectEntryInfo() {
		id = null;
		type = ProjectEntryType.Unknown;
		name = "";
	}

	public ProjectEntryInfo(ProjectEntryId id, ProjectEntryType type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
	}

	public ProjectEntryId getId() {
		return id;
	}

	public void setId(ProjectEntryId id) {
		this.id = id;
	}

	public ProjectEntryType getType() {
		return type;
	}

	public void setType(ProjectEntryType type) {
		this.type = type;
	}

	public ProjectEntryId getParent() {
		return parent;
	}

	public void setParent(ProjectEntryId parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
