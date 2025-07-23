package tom.tasks.daemon;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class VideoTranscriberConfig implements TaskConfig {

	private String file;

	public VideoTranscriberConfig() {
	}

	public VideoTranscriberConfig(Map<String, String> config) {
		// Video file - it's provided to us by the filesystem monitor, not the UI.
		file = config.get("File");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		// Don't add file because we will get that passed to us by the filesystem
		// monitor.
		return config;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
