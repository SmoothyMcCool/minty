package tom.document.service;

public class DocumentProcessingException extends RuntimeException {

	private static final long serialVersionUID = -4427363148385516603L;

	public DocumentProcessingException(String reason) {
		super(reason);
	}
}
