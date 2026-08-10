package tom.analytics.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserWorkflowSummaryId implements Serializable {

	private static final long serialVersionUID = -5225334209933953237L;

	@Column(name = "userId")
	private UUID userId;

	@Column(name = "workflowId")
	private UUID workflowId;

	public UserWorkflowSummaryId() {
	}

	public UserWorkflowSummaryId(UUID userId, UUID workflowId) {
		this.userId = userId;
		this.workflowId = workflowId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getWorkflowId() {
		return workflowId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public void setWorkflowId(UUID workflowId) {
		this.workflowId = workflowId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UserWorkflowSummaryId other)) {
			return false;
		}

		return Objects.equals(userId, other.userId) && Objects.equals(workflowId, other.workflowId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, workflowId);
	}
}