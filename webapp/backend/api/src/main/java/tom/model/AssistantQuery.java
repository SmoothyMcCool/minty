package tom.model;

import java.util.Objects;

import tom.api.AssistantId;
import tom.api.ConversationId;

public class AssistantQuery {

	private AssistantId assistantId;
	private ConversationId conversationId;
	private String query;

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

	public AssistantId getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(AssistantId assistantId) {
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
