package tom.api.model.document;

public class DocumentParsingException extends Exception {

	private static final long serialVersionUID = -2370008357249178324L;

	public DocumentParsingException(String reason) {
		super(reason);
	}
}
