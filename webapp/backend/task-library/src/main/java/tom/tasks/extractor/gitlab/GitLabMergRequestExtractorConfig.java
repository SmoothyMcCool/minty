package tom.tasks.extractor.gitlab;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class GitLabMergRequestExtractorConfig implements TaskConfig {

	private String baseUrl;
	private String projectId;
	private String mergeRequestId;
	private String privateToken;
	private Integer contextLines;
	private Integer maxLinesPerChunk;

	public GitLabMergRequestExtractorConfig() {
	}

	public GitLabMergRequestExtractorConfig(Map<String, String> config)
			throws JsonMappingException, JsonProcessingException {
		baseUrl = config.get("Base URL");
		projectId = config.get("Project ID");
		mergeRequestId = config.get("Merge Request ID");
		privateToken = config.get("Private Token");
		contextLines = Integer.parseInt(config.get("Number of Context Lines"));
		maxLinesPerChunk = Integer.parseInt(config.get("Maxiumum Lines Per Chunk"));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Base URL", TaskConfigTypes.String);
		config.put("Project ID", TaskConfigTypes.String);
		config.put("Merge Request ID", TaskConfigTypes.String);
		config.put("Private Token", TaskConfigTypes.String);
		config.put("Number of Context Lines", TaskConfigTypes.Number);
		config.put("Maxiumum Lines Per Chunk", TaskConfigTypes.Number);
		return config;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getMergeRequestId() {
		return mergeRequestId;
	}

	public String getPrivateToken() {
		return privateToken;
	}

	public Integer getContextLines() {
		return contextLines;
	}

	public Integer getMaxLinesPerChunk() {
		return maxLinesPerChunk;
	}

	public void updateFrom(Map<String, Object> input) {
		if (input.containsKey("Base URL")) {
			baseUrl = (String) input.get("Base URL");
		}
		if (input.containsKey("Project ID")) {
			projectId = (String) input.get("Project ID");
		}
		if (input.containsKey("Merge Request ID")) {
			mergeRequestId = (String) input.get("Merge Request ID");
		}
		if (input.containsKey("Private Token")) {
			privateToken = (String) input.get("Private Token");
		}
		if (input.containsKey("Number of Context Lines")) {
			contextLines = Integer.parseInt((String) input.get("Number of Context Lines"));
		}
		if (input.containsKey("Maxiumum Lines Per Chunk")) {
			maxLinesPerChunk = Integer.parseInt((String) input.get("Maxiumum Lines Per Chunk"));
		}
	}

}
