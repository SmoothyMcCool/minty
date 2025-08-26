package tom.model;

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

}
