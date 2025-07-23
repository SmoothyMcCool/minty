package tom.tasks.daemon;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Transcribe Video", configClass = "tom.tasks.daemon.VideoTranscriberConfig")
public class VideoTranscriber implements AiTask {

	private final Logger logger = LogManager.getLogger(VideoTranscriber.class);

	private final VideoTranscriberConfig configuration;

	public VideoTranscriber(VideoTranscriberConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "VideoTranscriber-" + configuration.getFile();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> results = new HashMap<>();
		results.put("result", configuration.getFile());
		return results;
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AiTask> runTask() {
		logger.info("Running transcription on " + configuration.getFile());
		Path file = Path.of(configuration.getFile());
		file.toFile().delete();
		return List.of();
	}

	@Override
	public void setInput(Map<String, String> input) {
		if (input.containsKey("File")) {
			configuration.setFile((input.get("File")));
		}
	}

}
