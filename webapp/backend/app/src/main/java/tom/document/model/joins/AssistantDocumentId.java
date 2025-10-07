package tom.document.model.joins;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class AssistantDocumentId implements Serializable {

	private static final long serialVersionUID = -3953209437140272990L;

	private UUID assistantId;
	private UUID documentId;

	public AssistantDocumentId() {
	}

	public AssistantDocumentId(UUID assistantId, UUID documentId) {
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

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	public UUID getDocumentId() {
		return documentId;
	}

	public void setDocumentId(UUID documentId) {
		this.documentId = documentId;
	}

}
