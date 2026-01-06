package tom.project.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import tom.api.NodeId;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;

@Entity
@Table(name = "ProjectNode")
public class Node {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UUID projectId;
	@Enumerated(EnumType.STRING)
	private NodeType type;
	private String name;
	private UUID parentId;
	private String data;
	private OffsetDateTime created;
	private OffsetDateTime updated;

	public Node() {

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

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
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

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(OffsetDateTime updated) {
		this.updated = updated;
	}

	public tom.api.model.project.Node toModel() {
		NodeInfo info = new NodeInfo();

		info.setCreated(getCreated());
		info.setNodeId(new NodeId(getId()));
		info.setName(getName());
		info.setParentId(new NodeId(getParentId()));
		info.setType(getType());
		info.setUpdated(getUpdated());
		return new tom.api.model.project.Node(info, getData());
	}

	@PrePersist
	public void onCreate() {
		created = OffsetDateTime.now();
		updated = created;
	}

	@PreUpdate
	public void onUpdate() {
		updated = OffsetDateTime.now();
	}

	public boolean isFile() {
		return type == NodeType.File;
	}

	public boolean isFolder() {
		return type == NodeType.Folder;
	}
}
