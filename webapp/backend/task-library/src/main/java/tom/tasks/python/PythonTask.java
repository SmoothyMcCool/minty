package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		doTheThing();
		return List.of();
	}

	@Override
	public void setInput(Map<String, String> input) {
		// Merge whatever we get with the input dictionary to pass to the python
		// interpreter.
		configuration.getInputDictionary().putAll(input);
	}

	private void doTheThing() {
		logger.info("doWork: Executing " + configuration.getPythonFile());
		result = taskServices.getPythonService().execute(configuration.getPythonFile(),
				configuration.getInputDictionary());
		logger.info("doWork: " + configuration.getPythonFile() + " completed.");
	}

	@Override
	public String expects() {
		return "This task simply provides whatever input and configuration"
				+ " is provided as a map to the associated Python file when it is run. "
				+ "What is expected is entirely up to the python you write.";
	}

	@Override
	public String produces() {
		return "This task doesn't produce output so it should be the last step in your workflow.";
	}
}
