package tom.document.model.joins;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import tom.api.AssistantId;
import tom.api.DocumentId;

@Embeddable
public class AssistantDocumentId implements Serializable {

	private static final long serialVersionUID = -3953209437140272990L;

	private AssistantId assistantId;
	private DocumentId documentId;

	public AssistantDocumentId() {
	}

	public AssistantDocumentId(AssistantId assistantId, DocumentId documentId) {
		this.assistantId = assistantId;
		this.documentId = documentId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AssistantDocumentId)) {
			return false;
		}
		AssistantDocumentId other = (AssistantDocumentId) o;
		return Objects.equals(assistantId, other.assistantId) && Objects.equals(documentId, other.documentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(assistantId, documentId);
	}

	public AssistantId getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(AssistantId assistantId) {
		this.assistantId = assistantId;
	}

	public DocumentId getDocumentId() {
		return documentId;
	}

	public void setDocumentId(DocumentId documentId) {
		this.documentId = documentId;
	}

}
