package tom;

public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7322543864612352169L;

	public NotFoundException(String reason) {
		super(reason);
	}

	public NotFoundException(String reason, Throwable cause) {
		super(reason, cause);
	}

}
