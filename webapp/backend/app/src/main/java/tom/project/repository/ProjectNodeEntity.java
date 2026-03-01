package tom.project.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeType;

@Entity
@Table(name = "ProjectNode", uniqueConstraints = @UniqueConstraint(columnNames = { "projectId", "path" }))
public class ProjectNodeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UserId ownerId;

	@Column(nullable = false)
	private UUID projectId;

	@Column
	private UUID parentId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 1024)
	private String path;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NodeType type;

	@Enumerated(EnumType.STRING)
	private FileType fileType;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false)
	private Instant created;

	@Column(nullable = false)
	private Instant updated;

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

	public UUID getProjectId() {
		return projectId;
	}

	public void setProjectId(UUID projectId) {
		this.projectId = projectId;
	}

	public UUID getParentId() {
		return parentId;
	}

	public void setParentId(UUID parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
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

}
