package tom.tasks.ewm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class WorkItemsFromQueryConfig implements TaskConfig {

	private String queryId;
	private String server;
	private String username;
	private String password;
	private String projectArea;
	private List<String> fields;

	public WorkItemsFromQueryConfig() {
	}

	public WorkItemsFromQueryConfig(Map<String, String> config) {
		queryId = config.get("Query ID");
		server = config.get("Server");
		username = config.get("Username");
		password = config.get("Password");
		projectArea = config.get("Project Area");
		fields = Arrays.asList(config.get("Fields").split(","));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Server", TaskConfigTypes.String);
		config.put("Project Area", TaskConfigTypes.String);
		config.put("Query ID", TaskConfigTypes.String);
		config.put("Username", TaskConfigTypes.String);
		config.put("Password", TaskConfigTypes.String);
		config.put("Fields", TaskConfigTypes.String);
		return config;
	}

	public String getQueryId() {
		return queryId;
	}

	public String getServer() {
		return server;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public List<String> getFields() {
		return fields;
	}

	public String getProjectArea() {
		return projectArea;
	}
}
