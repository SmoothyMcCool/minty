package tvm.tasks.daemon;

import java.util.HashMap;
import java.util.Map;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class VideoTranscriberConfig implements AiTaskConfig {

	private String file;

	public VideoTranscriberConfig() {
	}

	public VideoTranscriberConfig(Map<String, String> config) {
		// Read out file - it's provided to us by the filesystem monitor.
		file = config.get("file");
	}

	@Override
	public Map<String, AiTaskConfigTypes> getConfig() {
		Map<String, AiTaskConfigTypes> config = new HashMap<>();
		// Don't add file because we will get that passed to us by the filesystem
		// monitor.
		return config;
	}

	public String getFile() {
		return file;
	}
}
