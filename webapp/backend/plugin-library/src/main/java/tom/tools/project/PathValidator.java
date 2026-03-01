package tom.tools.project;

public final class PathValidator {

	private PathValidator() {
	}

	public static void validate(String path) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("Path must not be empty.");
		}

		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("Path must be absolute and start with '/'.");
		}

		if (path.contains("..")) {
			throw new IllegalArgumentException("Path must not contain '..'.");
		}

		if (path.contains("//")) {
			throw new IllegalArgumentException("Path must not contain '//'.");
		}

		if (path.length() > 1024) {
			throw new IllegalArgumentException("Path exceeds maximum length.");
		}
	}
}
