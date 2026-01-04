package tom.config;

public class PropertyNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -7913828472404904782L;

	public PropertyNotFoundException(String reason) {
		super(reason);
	}
}
