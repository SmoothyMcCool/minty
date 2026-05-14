package tom.meta.model;

/**
 * Mirrors the request_status lookup table. Used throughout the application
 * layer for type safety.
 */
public enum RequestStatus {
	QUEUED, PROCESSING, COMPLETED, FAILED;

	/** Returns the lowercase string value stored in the database. */
	public String toDbValue() {
		return name().toLowerCase();
	}

	public static RequestStatus fromDbValue(String value) {
		return valueOf(value.toUpperCase());
	}
}
