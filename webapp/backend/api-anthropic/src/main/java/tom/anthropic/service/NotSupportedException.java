package tom.anthropic.service;

public class NotSupportedException extends RuntimeException {

	private static final long serialVersionUID = -8598165422667917543L;

	public NotSupportedException(String reason) {
		super(reason);
	}
}
