package tom.tools.datetime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;

import tom.tool.MintyTool;

public class CurrentTimeTool implements MintyTool {

	private static final Logger logger = LogManager.getLogger(CurrentTimeTool.class);

	@Tool(description = "Get the current local time")
	String getCurrentLocalTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
	}

	@Override
	public String name() {
		return "Get Local Time";
	}

	@Override
	public String description() {
		return "Returns the local time, formatted as an ISO Date Time";
	}

}
