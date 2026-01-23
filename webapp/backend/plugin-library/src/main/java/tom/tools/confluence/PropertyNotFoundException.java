package tom.tools.confluence;

public class PropertyNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PropertyNotFoundException(String reason) {
		super(reason);
	}

	public PropertyNotFoundException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public PropertyNotFoundException(Throwable cause) {
		super(cause);
	}
}
