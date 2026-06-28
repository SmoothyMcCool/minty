package tom.llm;

public class KeyRequiredException extends RuntimeException {

	private static final long serialVersionUID = -1309258669514929079L;

	public KeyRequiredException(String reason) {
		super(reason);
	}
}
