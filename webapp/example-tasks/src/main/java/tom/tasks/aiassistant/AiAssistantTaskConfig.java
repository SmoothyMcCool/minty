package tom.tasks.aiassistant;

import java.util.HashMap;
import java.util.Map;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class AiAssistantTaskConfig implements AiTaskConfig {

	private int assistant;
	private String rtcQueryId;
	private String prompt;

	public AiAssistantTaskConfig() {
		assistant = 0;
		rtcQueryId = "";
		prompt = "";
	}

	public AiAssistantTaskConfig(Map<String, String> config) {
		assistant = Integer.parseInt(config.get("assistant"));
		rtcQueryId = config.get("RTC Query ID");
		prompt = config.get("prompt");
	}

	@Override
	public Map<String, AiTaskConfigTypes> getConfig() {
		Map<String, AiTaskConfigTypes> cfg = new HashMap<>();
		cfg.put("assistant", AiTaskConfigTypes.AssistantIdentifier);
		cfg.put("RTC Query ID", AiTaskConfigTypes.String);
		cfg.put("prompt", AiTaskConfigTypes.String);
		return cfg;
	}

	public int getAssistant() {
		return assistant;
	}

	public String getRtcQueryId() {
		return rtcQueryId;
	}

	public String getPrompt() {
		return prompt;
	}
}
