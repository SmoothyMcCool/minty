package tom.tools.toolregistry;

public class DuplicateToolException extends Exception {

	private static final long serialVersionUID = 1399345187906304117L;

	public DuplicateToolException(String reason) {
		super(reason);
	}
}
