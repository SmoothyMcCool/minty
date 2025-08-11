package tom.document.model.joins;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class AssistantDocumentId implements Serializable {

	private static final long serialVersionUID = -3953209437140272990L;

	private int assistantId;
	private String documentId;

	public AssistantDocumentId() {
	}

	public AssistantDocumentId(int assistantId, String documentId) {
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

	public int getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(int assistantId) {
		this.assistantId = assistantId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

}
