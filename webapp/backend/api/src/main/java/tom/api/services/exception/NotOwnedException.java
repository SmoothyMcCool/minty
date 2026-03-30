package tom.api.services.exception;

public class NotOwnedException extends Exception {

	private static final long serialVersionUID = 7322543864612352169L;

	public NotOwnedException(String reason) {
		super(reason);
	}

	public NotOwnedException(String reason, Throwable cause) {
		super(reason, cause);
	}

}
