package tom.tasks.transform.python;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.services.python.PythonException;
import tom.api.services.python.PythonResult;
import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

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

@RunnableTask
public class PythonExecutor implements MintyTask, ServiceConsumer {

	private List<? extends OutputPort> outputs;

	private TaskLogger logger;
	private PythonExecutorConfig configuration;
	private Packet result;
	private PluginServices pluginServices;
	private String error;
	private Packet input;
	private boolean failed;

	public PythonExecutor() {
		result = null;
		pluginServices = null;
		error = null;
		input = null;
		outputs = null;
		failed = false;
	}

	public PythonExecutor(PythonExecutorConfig configuration) {
		this();
		this.configuration = configuration;
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		result = new Packet();
		result.setId(input.getId());

		try {
			if (StringUtils.isBlank(configuration.getPython())) {
				logger.warn("No python code provided to task!");
				error = "No python code provided to task.";
				failed = true;
				return;
			}

			logger.debug("PythonExecutor: Executing " + configuration.getPython());

			PythonResult pyResult = pluginServices.getPythonService().executeCodeString(configuration.getPython(),
					input);

			logger.info("Python complete. Logs:");
			pyResult.logs().forEach(log -> logger.info(log));
			try {
				result.setText((List<String>) pyResult.result().get("text"));
			} catch (Exception e) {
				logger.warn("Text element returned from python is invalid. Should be a List<String>");
			}
			try {
				result.setData((List<Map<String, Object>>) pyResult.result().get("data"));
			} catch (Exception e) {
				logger.warn("Data element returned from python is invalid. Should be a List<Map<String, Object>>)");
			}

		} catch (PythonException e) {
			logger.warn("PythonExecutor: Caught exception while running python:", e);
			if (e.getLogs() != null) {
				e.getLogs().forEach(log -> logger.info(log));
			}
			failed = true;
		} catch (Exception e) {
			logger.warn("PythonExecutor: Caught exception while running python:", e);
			failed = true;
		}

		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. PythonExecutor should only ever have exactly one input!");
		}

		input = dataPacket;
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return input != null;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Execute python Code, taking a Packet as input.";
			}

			@Override
			public String expects() {
				return "This task simply provides whatever input and configuration"
						+ " is provided as a map to the associated Python file when it is run. "
						+ "What is expected is entirely up to the python you provide.";
			}

			@Override
			public String produces() {
				return "The output from python. The output must conform to the Packet JSON structure.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new PythonExecutorConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				try {
					return new PythonExecutorConfig(configuration);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to read configuration. ", e);
				}
			}

			@Override
			public String taskName() {
				return "Execute Python";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
			}

		};
	}

	@Override
	public void inputTerminated(int i) {
		// Nothing to do.
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}
}
