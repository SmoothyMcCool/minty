package tom.workflow.model.joins;

import java.util.UUID;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import tom.api.UserId;
import tom.workflow.repository.Workflow;

@Entity
@Table(name = "UserWorkflowLinks")
public class UserWorkflowLink {

	@EmbeddedId
	private UserWorkflowId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("workflowId")
	@JoinColumn(name = "workflowId", nullable = false)
	private Workflow workflow;

	public UserWorkflowLink() {
	}

	public UserWorkflowLink(UserId userId, Workflow workflow) {
		this.workflow = workflow;
		id = new UserWorkflowId(userId.getValue(), workflow.getId());
	}

	public UserWorkflowId getId() {
		return id;
	}

	public void setId(UserWorkflowId id) {
		this.id = id;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}
}
