package tom.document.model.joins;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import tom.assistant.repository.Assistant;
import tom.document.model.MintyDoc;

@Entity
@Table(name = "DocumentAssistantLinks")
public class AssistantDocumentLink {
	AssistantDocumentId id;

	private Assistant assistant;

	private MintyDoc document;

	public AssistantDocumentLink() {
	}

	public AssistantDocumentLink(Assistant assistant, MintyDoc document) {
		this.assistant = assistant;
		this.document = document;
		id = new AssistantDocumentId(assistant.getId().value(), document.getDocumentId().value());
	}

	@EmbeddedId
	public AssistantDocumentId getId() {
		return id;
	}

	public void setId(AssistantDocumentId id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("assistantId")
	@JoinColumn(name = "assistantId", nullable = false)
	public Assistant getAssistant() {
		return assistant;
	}

	public void setAssistant(Assistant assistant) {
		this.assistant = assistant;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("documentId")
	@JoinColumn(name = "documentId", nullable = false)
	public MintyDoc getDocument() {
		return document;
	}

	public void setDocument(MintyDoc document) {
		this.document = document;
	}
}
