package tom.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AssistantDailyUsageId implements Serializable {
	private static final long serialVersionUID = 3704547776009656884L;

	@Column(name = "day")
	private LocalDate day;

	@Column(name = "assistantId")
	private UUID assistantId;

	public AssistantDailyUsageId() {
	}

	public AssistantDailyUsageId(LocalDate day, UUID assistantId) {
		this.day = day;
		this.assistantId = assistantId;
	}

	public LocalDate getDay() {
		return day;
	}

	public void setDay(LocalDate day) {
		this.day = day;
	}

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof AssistantDailyUsageId other)) {
			return false;
		}

		return Objects.equals(day, other.day) && Objects.equals(assistantId, other.assistantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(day, assistantId);
	}
}
