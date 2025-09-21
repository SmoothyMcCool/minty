package tom.document.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import tom.tag.model.MintyTag;

@Entity(name = "Document")
public class MintyDoc {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID documentId;
	private String title;
	private DocumentState state = DocumentState.NO_CONTENT;
	private UUID ownerId;
	@Transient
	private List<UUID> associatedAssistantIds;
	@ManyToMany(mappedBy = "associatedDocuments", fetch = FetchType.EAGER)
	private List<MintyTag> associatedTags;

	public MintyDoc() {
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

	public UUID getDocumentId() {
		return documentId;
	}

	public void setDocumentId(UUID documentId) {
		this.documentId = documentId;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	@Transient
	public List<UUID> getAssociatedAssistantIds() {
		return associatedAssistantIds;
	}

	public void setAssociatedAssistants(List<UUID> associatedAssistantIds) {
		this.associatedAssistantIds = associatedAssistantIds;
	}

	public List<MintyTag> getAssociatedTags() {
		return associatedTags;
	}

	public void setAssociatedTags(List<MintyTag> associatedTags) {
		this.associatedTags = associatedTags;
	}

}
