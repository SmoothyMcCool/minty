package tom.workflow.model.joins;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserWorkflowId implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 304838745383109854L;

	@Column(name = "userId")
	private UUID userId;

	@Column(name = "workflowId")
	private UUID workflowId;

	public UserWorkflowId() {
	}

	public UserWorkflowId(UUID userId, UUID workflowId) {
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
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UserWorkflowId))
			return false;
		UserWorkflowId that = (UserWorkflowId) o;
		return Objects.equals(userId, that.userId) && Objects.equals(workflowId, that.workflowId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, workflowId);
	}
}