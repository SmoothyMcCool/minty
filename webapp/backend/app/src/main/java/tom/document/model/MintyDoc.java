package tom.document.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import tom.api.DocumentId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.document.Document;

@Entity(name = "Document")
public class MintyDoc {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private String title;
	private UserId ownerId;
	private ProjectId projectId;
	private boolean vectorized;
	private String summary;
	@Column(nullable = false)
	private Instant created;
	@Column(nullable = false)
	private Instant updated;

	@OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("sequenceOrder ASC")
	private List<MintyDocSegment> segments = new ArrayList<>();

	public MintyDoc() {
	}

	// Lifecycle Hooks
	@PrePersist
	protected void onCreate() {
		created = Instant.now();
		updated = Instant.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updated = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

	public ProjectId getProjectId() {
		return projectId;
	}

	public void setProjectId(ProjectId projectId) {
		this.projectId = projectId;
	}

	public boolean isVectorized() {
		return vectorized;
	}

	public void setVectorized(boolean vectorized) {
		this.vectorized = vectorized;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
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

	public List<MintyDocSegment> getSegments() {
		return segments;
	}

	public void setSegments(List<MintyDocSegment> segments) {
		this.segments = segments;
	}

	public Document fromEntity() {
		return new tom.api.model.document.Document(new DocumentId(id), title, ownerId, projectId, vectorized, created,
				updated, summary, getSegments().stream().map(segment -> segment.fromEntityNoContent()).toList());
	}

}
