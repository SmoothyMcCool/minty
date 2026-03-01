package tom.project.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import tom.api.UserId;

@Entity
@Table(name = "ProjectFileContent", uniqueConstraints = @UniqueConstraint(columnNames = { "nodeId", "version" }))
public class ProjectFileContent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UserId ownerId;

	@Column(nullable = false)
	private UUID nodeId;

	@Column(nullable = false)
	private int version;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private Instant created;

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

	public UUID getNodeId() {
		return nodeId;
	}

	public void setNodeId(UUID nodeId) {
		this.nodeId = nodeId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

}
