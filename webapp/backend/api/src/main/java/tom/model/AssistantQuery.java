package tom.model;

import java.util.Objects;
import java.util.UUID;

public class AssistantQuery {

	private UUID assistantId;
	private UUID conversationId;
	private String query;

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(assistantId, conversationId, query);
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
		return Objects.equals(assistantId, other.assistantId) && Objects.equals(conversationId, other.conversationId)
				&& Objects.equals(query, other.query);
	}

}
