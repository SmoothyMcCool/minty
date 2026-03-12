package tom.workflow.executor;

import java.nio.file.Path;

import tom.api.task.TaskLogger;

public class NullWorkflowLogger implements TaskLogger {

	public static NullWorkflowLogger NullLogger = new NullWorkflowLogger(null, "");

	public NullWorkflowLogger(Path path, String unsanitizedFilename) {
	}

	@Override
	public void trace(String message) {
	}

	@Override
	public void debug(String message) {
	}

	@Override
	public void info(String message) {
	}

	@Override
	public void error(String message) {
	}

	@Override
	public void warn(String message) {
	}

	@Override
	public void info(String message, Throwable e) {
	}

	@Override
	public void error(String message, Throwable e) {
	}

	@Override
	public void warn(String message, Throwable e) {
	}

	@Override
	public void debug(String message, Throwable e) {
	}

	@Override
	public void trace(String message, Throwable e) {
	}

	public String getFileName() {
		return "";
	}

	@Override
	public void setLoggingLevel(LogLevel level) {
	}

}
