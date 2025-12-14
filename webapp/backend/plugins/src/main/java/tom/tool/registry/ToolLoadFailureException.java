package tom.tool.registry;

public class ToolLoadFailureException extends Exception {

	private static final long serialVersionUID = 1399345187906304117L;

	public ToolLoadFailureException(String reason) {
		super(reason);
	}

	public ToolLoadFailureException(Throwable cause) {
		super(cause);
	}
}
