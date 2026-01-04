package tom.api.model.assistant;

import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;

import tom.api.ConversationId;

public class AssistantQuery {

	private AssistantSpec assistantSpec;
	private ConversationId conversationId;
	private String query;
	private int contextSize;
	private ByteArrayResource imageData;
	private String imageContentType;

	public ConversationId getConversationId() {
		return conversationId;
	}

	public void setConversationId(ConversationId conversationId) {
		this.conversationId = conversationId;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public AssistantSpec getAssistantSpec() {
		return assistantSpec;
	}

	public void setAssistantSpec(AssistantSpec assistantSpec) {
		this.assistantSpec = assistantSpec;
	}

	public int getContextSize() {
		return contextSize;
	}

	public void setContextSize(int contextSize) {
		this.contextSize = contextSize;
	}

	@Override
	public int hashCode() {
		return Objects.hash(assistantSpec, conversationId, query);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssistantQuery other = (AssistantQuery) obj;
		return Objects.equals(assistantSpec, other.assistantSpec)
				&& Objects.equals(conversationId, other.conversationId) && Objects.equals(query, other.query);
	}

	public void setImage(ByteArrayResource imageData, String contentType) {
		this.imageData = imageData;
		this.imageContentType = contentType;
	}

	public ByteArrayResource getImageData() {
		return this.imageData;
	}

	public String getContentType() {
		return imageContentType;
	}

}
