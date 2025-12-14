package tom.api.task;

public interface TaskLogger {

	void info(String message);

	void info(String message, Throwable e);

	void error(String message);

	void error(String message, Throwable e);

	void warn(String message);

	void warn(String message, Throwable e);

	void debug(String string);

	void debug(String message, Throwable e);

}