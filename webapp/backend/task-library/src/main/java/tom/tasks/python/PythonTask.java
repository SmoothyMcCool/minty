package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.services.TaskServices;
import tom.task.MintyTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

/*
import json
import sys

def read_dict(inFile):
	with open(inFile, 'r') as f:
		return json.load(f)
	except FileNotFoundError:
		exit(-1)
	except json.JSONDecodeError:
		exit(-1)

def write_to_file(data, outFile):
	with open(filename, "w") as file:
		json.dump(data, file, indent=4)

def main():
	if (len(sys.argv) == 3):
		data = read_dict(sys.argv[1])
		write_to_file(data, sys.argv[2])

if __name__ == "__main__":
	main()
*/

@PublicTask(name = "Execute Python", configClass = "tom.tasks.python.PythonTaskConfig")
public class PythonTask implements MintyTask, ServiceConsumer {

	private final Logger logger = LogManager.getLogger(PythonTask.class);

	private final PythonTaskConfig configuration;
	private Map<String, Object> result = new HashMap<>();
	private TaskServices taskServices = null;
	private String error = null;
	private Map<String, Object> input = Map.of();

	public PythonTask() {
		configuration = new PythonTaskConfig();
	}

	public PythonTask(PythonTaskConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "PythonTask-" + configuration.getPython();
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
			logger.debug("doWork: Executing " + configuration.getPython());

			if (input.containsKey("Code")) {

				String code = input.get("Code").toString();
				result = taskServices.getPythonService().executeCodeString(code, input);
				logger.info("doWork: completed execution of input-provided code.");

			} else {

				result = taskServices.getPythonService().executeCodeString(configuration.getPython(), input);
				logger.info("doWork: python completed.");

			}

			if (input.containsKey("ConversationId")) {
				result.put(null, input.get("ConversationId"));
			}
		} catch (Exception e) {
			error = "Caught exception while running python: " + e.toString();
			return List.of();
		}

		return List.of(result);
	}

	@Override
	public void setInput(Map<String, Object> input) {
		this.input = input;
	}

	@Override
	public String expects() {
		return "This task simply provides whatever input and configuration"
				+ " is provided as a map to the associated Python file when it is run. "
				+ "What is expected is entirely up to the python you write.\n\nThe one "
				+ "exception is that if the input contains a key \"Code\", that will be "
				+ "interpretted as the code to run, overriding any code provided in configuration.";
	}

	@Override
	public String produces() {
		return "The output of the python. If the input contained a ConversationId, that "
				+ "is propagated out, for further AI processing fun.";
	}
}
