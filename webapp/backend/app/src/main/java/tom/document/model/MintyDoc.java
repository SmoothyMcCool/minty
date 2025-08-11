package tom.document.model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity(name = "Document")
public class MintyDoc {

	@Id
	private String documentId;
	private String title;
	private DocumentState state = DocumentState.NO_CONTENT;
	private String model;
	private int ownerId;
	@Transient
	private List<Integer> associatedAssistantIds;

	public MintyDoc() {}

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

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	@Transient
	public List<Integer> getAssociatedAssistantIds() {
		return associatedAssistantIds;
	}

	public void setAssociatedAssistants(List<Integer> associatedAssistantIds) {
		this.associatedAssistantIds = associatedAssistantIds;
	}

}
