package tom.tasks.transform.python;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.services.TaskServices;
import tom.api.services.python.PythonException;
import tom.api.services.python.PythonResult;
import tom.model.ServiceConsumer;
import tom.task.MintyTask;
import tom.task.OutputPort;
import tom.task.Packet;
import tom.task.TaskConfigSpec;
import tom.task.TaskLogger;
import tom.task.TaskSpec;
import tom.task.annotation.RunnableTask;

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
	private TaskServices taskServices;
	private String error;
	private Packet input;
	private boolean failed;

	public PythonExecutor() {
		result = null;
		taskServices = null;
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
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public void run() {
		result = new Packet();
		result.setId(input.getId());

		try {
			logger.debug("PythonExecutor: Executing " + configuration.getPython());

			PythonResult pyResult = taskServices.getPythonService().executeCodeString(configuration.getPython(),
					input.getData());

			logger.info("Python complete. Logs:");
			pyResult.logs().forEach(log -> logger.info(log));
			result.addData(pyResult.result());

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
		if (dataPacket.getData().size() != 1) {
			failed = true;
			throw new RuntimeException("Packet must contain exactly one data element.");
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
			public String expects() {
				return "This task simply provides whatever input and configuration"
						+ " is provided as a map to the associated Python file when it is run. "
						+ "What is expected is entirely up to the python you provide.";
			}

			@Override
			public String produces() {
				return "The output of the python. If the input contained a ConversationId, that "
						+ "is propagated out, for further processing fun.";
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
			public TaskConfigSpec taskConfiguration(Map<String, String> configuration) {
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
				return "Transform";
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
