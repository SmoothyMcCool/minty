package tom.tasks.emit.confluence;

public class StringListFormatException extends RuntimeException {

	private static final long serialVersionUID = 2928427372749626297L;

	public StringListFormatException(String reason) {
		super(reason);
	}

	public StringListFormatException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
