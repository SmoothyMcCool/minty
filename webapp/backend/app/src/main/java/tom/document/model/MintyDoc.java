package tom.document.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.DocumentId;
import tom.api.ProjectId;
import tom.api.UserId;

@Entity(name = "Document")
public class MintyDoc {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private String title;
	private DocumentState state = DocumentState.NO_CONTENT;
	private UserId ownerId;
	private ProjectId projectId;
	private boolean vectorized;

	public MintyDoc() {
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

	public DocumentState getState() {
		return state;
	}

	public void setState(DocumentState state) {
		this.state = state;
	}

	public DocumentId getDocumentId() {
		return new DocumentId(id);
	}

	public void setDocumentId(DocumentId id) {
		this.id = id == null ? null : id.value();
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

}
