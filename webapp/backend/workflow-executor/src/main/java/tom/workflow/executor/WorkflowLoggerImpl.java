package tom.workflow.executor;

import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;

import tom.api.task.TaskLogger;

public class WorkflowLoggerImpl implements AutoCloseable, TaskLogger {
	private static final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
	private static final Configuration cfg = ctx.getConfiguration();

	private final FileAppender appender;
	private final Path filePath;
	private final String fileName;

	public WorkflowLoggerImpl(Path path, String unsanitizedFilename) {
		PatternLayout layout = PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] %-5p - %m%n")
				.withConfiguration(cfg).build();

		fileName = sanitize(unsanitizedFilename);
		filePath = path.resolve(fileName);
		this.appender = FileAppender.newBuilder().setName("FileLogger-" + fileName).withFileName(filePath.toString())
				.setLayout(layout).withAppend(true).setConfiguration(cfg).build();

		this.appender.start();
	}

	private static String sanitize(String filename) {
		if (filename == null || filename.isBlank()) {
			return "blank";
		}
		String sanitized = filename.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\p{Cntrl}", "").trim();
		if (sanitized.isEmpty()) {
			sanitized = "unnamed";
		}
		if (sanitized.length() > 255) {
			sanitized = sanitized.substring(0, 255);
		}
		return sanitized;
	}

	private void log(Level level, String message) {
		LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("dynamic").setLevel(level)
				.setMessage(new SimpleMessage(message)).build();
		appender.append(event);
	}

	private void log(Level level, String message, Throwable e) {
		LogEvent event = Log4jLogEvent.newBuilder().setLoggerName("dynamic").setLevel(level)
				.setMessage(new SimpleMessage(message)).setThrown(e).build();
		appender.append(event);
	}

	@Override
	public void debug(String message) {
		log(Level.DEBUG, message);
	}

	@Override
	public void info(String message) {
		log(Level.INFO, message);
	}

	@Override
	public void error(String message) {
		log(Level.ERROR, message);
	}

	@Override
	public void warn(String message) {
		log(Level.WARN, message);
	}

	@Override
	public void close() {
		appender.stop();
	}

	@Override
	public void info(String message, Throwable e) {
		log(Level.INFO, message, e);
	}

	@Override
	public void error(String message, Throwable e) {
		log(Level.ERROR, message, e);
	}

	@Override
	public void warn(String message, Throwable e) {
		log(Level.WARN, message, e);
	}

	@Override
	public void debug(String message, Throwable e) {
		log(Level.DEBUG, message, e);
	}

	public String getFileName() {
		return fileName;
	}

}
