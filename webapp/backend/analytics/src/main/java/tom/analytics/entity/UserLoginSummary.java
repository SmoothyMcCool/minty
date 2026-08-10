package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserLoginSummary")
public class UserLoginSummary {
	@Id
	@Column(name = "userId")
	private UUID userId;

	@Column(name = "account")
	private String account;

	@Column(name = "totalLogins")
	private long totalLogins;

	@Column(name = "lastLogin")
	private LocalDateTime lastLogin;

	@Column(name = "loginsLastWeek")
	private long loginsLastWeek;

	@Column(name = "loginsLastMonth")
	private long loginsLastMonth;

	@Column(name = "loginsLastYear")
	private long loginsLastYear;

	public UserLoginSummary() {
	}

	public UUID getUserId() {
		return userId;
	}

	public String getAccount() {
		return account;
	}

	public long getTotalLogins() {
		return totalLogins;
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public long getLoginsLastWeek() {
		return loginsLastWeek;
	}

	public long getLoginsLastMonth() {
		return loginsLastMonth;
	}

	public long getLoginsLastYear() {
		return loginsLastYear;
	}
}
