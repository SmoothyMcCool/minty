package tom.analytics.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "DailySystemStatistics")
public class DailySystemStatistics {

	@Id
	@Column(name = "day")
	private LocalDate day;

	@Column(name = "logins")
	private long logins;

	@Column(name = "conversations")
	private long conversations;

	@Column(name = "messages")
	private long messages;

	public DailySystemStatistics() {
	}

	public LocalDate getDay() {
		return day;
	}

	public long getLogins() {
		return logins;
	}

	public long getConversations() {
		return conversations;
	}

	public long getMessages() {
		return messages;
	}
}