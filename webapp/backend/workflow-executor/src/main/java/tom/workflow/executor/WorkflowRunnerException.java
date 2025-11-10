package tom.workflow.executor;

public class WorkflowRunnerException extends RuntimeException {

	private static final long serialVersionUID = 3379252813522689742L;

	public WorkflowRunnerException(String reason) {
		super(reason);
	}

	public WorkflowRunnerException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
