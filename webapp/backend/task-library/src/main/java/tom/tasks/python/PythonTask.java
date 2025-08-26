package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.services.TaskServices;
import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Execute Python", configClass = "tom.tasks.python.PythonTaskConfig")
public class PythonTask implements AiTask, ServiceConsumer {

	private final Logger logger = LogManager.getLogger(PythonTask.class);

	private final PythonTaskConfig configuration;
	private Map<String, Object> result = new HashMap<>();
	private TaskServices taskServices = null;
	private String error = null;

	public PythonTask() {
		configuration = new PythonTaskConfig();
	}

	public PythonTask(PythonTaskConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "PythonTask-" + configuration.getPythonFile();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> map = new HashMap<>();
		map.put("result", result);
		return map;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		try {
			logger.debug("doWork: Executing " + configuration.getPythonFile());

			if (configuration.getInputDictionary().containsKey("Data")) {

				String code = configuration.getInputDictionary().get("Data").toString();
				result = taskServices.getPythonService().executeCodeString(code, configuration.getInputDictionary());

				logger.info("doWork: completed execution of input-provided code.");

			} else {

				result = taskServices.getPythonService().execute(configuration.getPythonFile(),
						configuration.getInputDictionary());
				logger.info("doWork: " + configuration.getPythonFile() + " completed.");
			}

			if (configuration.getInputDictionary().containsKey("ConversationId")) {
				result.put(null, configuration.getInputDictionary().get("ConversationId"));
			}
		} catch (Exception e) {
			error = "Caught exception while running python: " + e.toString();
			return List.of();
		}

		return List.of(result);
	}

	@Override
	public void setInput(Map<String, Object> input) {
		// Merge whatever we get with the input dictionary to pass to the python
		// interpreter.
		configuration.getInputDictionary().putAll(input);
	}

	@Override
	public String expects() {
		return "This task simply provides whatever input and configuration"
				+ " is provided as a map to the associated Python file when it is run. "
				+ "What is expected is entirely up to the python you write.\n\nThe one "
				+ "exception is that if the input contains a key \"Data\", that will be "
				+ "interpretted as the code to run, overriding any filename provided.";
	}

	@Override
	public String produces() {
		return "If the input contained a ConversationId, that is propagated out, for further AI processing fun.";
	}
}
