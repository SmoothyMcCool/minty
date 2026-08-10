package tom.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SystemOverview")
public class SystemOverview {
	@Id
	private Integer id = 1;

	@Column(name = "users")
	private long users;

	@Column(name = "assistants")
	private long assistants;

	@Column(name = "conversations")
	private long conversations;

	@Column(name = "messages")
	private long messages;

	@Column(name = "logins")
	private long logins;

	@Column(name = "workflowRuns")
	private long workflowRuns;

	@Column(name = "llmRequests")
	private long llmRequests;

	public SystemOverview() {
	}

	public Integer getId() {
		return id;
	}

	public long getUsers() {
		return users;
	}

	public long getAssistants() {
		return assistants;
	}

	public long getConversations() {
		return conversations;
	}

	public long getMessages() {
		return messages;
	}

	public long getLogins() {
		return logins;
	}

	public long getWorkflowRuns() {
		return workflowRuns;
	}

	public long getLlmRequests() {
		return llmRequests;
	}
}
