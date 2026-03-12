package tom.api.task;

public interface TaskLogger {

	public enum LogLevel {
		TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);

		private final int level;

		LogLevel(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}

		public static LogLevel fromString(String logLevel) {
			if (logLevel.equalsIgnoreCase("TRACE")) {
				return TRACE;
			}
			if (logLevel.equalsIgnoreCase("DEBUG")) {
				return DEBUG;
			}
			if (logLevel.equalsIgnoreCase("INFO")) {
				return INFO;
			}
			if (logLevel.equalsIgnoreCase("WARN")) {
				return WARN;
			}
			if (logLevel.equalsIgnoreCase("ERROR")) {
				return ERROR;
			}
			return DEBUG;
		}
	}

	void info(String message);

	void info(String message, Throwable e);

	void error(String message);

	void error(String message, Throwable e);

	void warn(String message);

	void warn(String message, Throwable e);

	void debug(String string);

	void debug(String message, Throwable e);

	void trace(String string);

	void trace(String message, Throwable e);

	void setLoggingLevel(LogLevel level);
}