package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;
import tom.task.services.TaskServices;

@PublicTask(name = "Execute Python", configClass = "tom.tasks.python.PythonTaskConfig")
public class PythonTask implements AiTask, ServiceConsumer {

	private final Logger logger = LogManager.getLogger(PythonTask.class);

	private final PythonTaskConfig configuration;
	private Map<String, Object> result = new HashMap<>();
	private TaskServices taskServices = null;

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
	public List<AiTask> runTask() {
		doTheThing();
		return List.of();
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		Map<String, String> response = doTheThing();
		return List.of(response);
	}

	@Override
	public void setInput(Map<String, String> input) {
		// Merge whatever we get with the input dictionary to pass to the python
		// interpreter.
		configuration.getInputDictionary().putAll(input);
	}

	private Map<String, String> doTheThing() {
		logger.info("doWork: Executing " + configuration.getPythonFile());

		if (configuration.getInputDictionary().containsKey("Data")) {

			String code = configuration.getInputDictionary().get("Data");
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

		ObjectMapper mapper = new ObjectMapper();
		return result.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
			try {
				return mapper.writeValueAsString(e.getValue());
			} catch (Exception except) {
				return String.valueOf(e.getValue());
			}
		}));

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
