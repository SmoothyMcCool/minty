package tom.workflow.executor;

public class TerminalTaskError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TerminalTaskError(String reason) {
		super(reason);
	}

	public TerminalTaskError(String reason, Throwable cause) {
		super(reason, cause);
	}
}
