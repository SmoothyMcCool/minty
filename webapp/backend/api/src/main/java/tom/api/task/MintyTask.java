package tom.api.task;

import java.util.List;

public abstract class MintyTask implements Runnable {

	private String stepName = "";
	private TaskLogger logger = null;

	// The results of this Task. This may be, but is not necessarily the same as the
	// output of runTask. This result represents all or a portion of the final
	// output of the overall workflow (for example, information retrieved from a
	// webpage during this Task).
	public abstract Packet getResult();

	// If any error occured, output it here to aid in debugging.
	public abstract String getError();

	// Returns true if this task will consume the packet if provided, false
	// otherwise.
	// This is to accommodate use cases such as grouping, where if a key does not
	// match the current group, we might not want the packet. Rejecting a packet
	// will cause the workflow to assume that this task does not want more packets
	// on the given input.
	public boolean wantsInput(int inputNum, Packet dataPacket) {
		return true;
	}

	// Provide a piece of input to the task. The task should return false if it does
	// not yet have all the input it needs for that input number, true if it does.
	public abstract boolean giveInput(int inputNum, Packet dataPacket);

	public abstract void setOutputConnectors(List<? extends OutputPort> outputs);

	// Return true if all inputs have all the required packets, false otherwise.
	public abstract boolean readyToRun();

	public abstract TaskSpec getSpecification();

	// This is called when there is no more input to available for this input.
	// (Upstream producer has completed).
	public abstract void inputTerminated(int i);

	public abstract boolean failed();

	final public void setLogger(TaskLogger logger) {
		this.logger = logger;
	}

	public String html() {
		return "";
	}

	public boolean terminalFailure() {
		return false;
	}

	public boolean stepComplete() {
		return false;
	}

	public void setName(String stepName) {
		this.stepName = stepName;
	}

	final protected void info(String message) {
		logger.info(stepName + ": " + message);
	}

	final protected void info(String message, Throwable e) {
		logger.info(stepName + ": " + message, e);
	}

	final protected void error(String message) {
		logger.error(stepName + ": " + message);
	}

	final protected void error(String message, Throwable e) {
		logger.error(stepName + ": " + message, e);
	}

	final protected void warn(String message) {
		logger.warn(stepName + ": " + message);
	}

	final protected void warn(String message, Throwable e) {
		logger.warn(stepName + ": " + message, e);
	}

	final protected void debug(String string) {
		logger.debug(stepName + ": " + string);
	}

	final protected void debug(String message, Throwable e) {
		logger.debug(stepName + ": " + message, e);
	}

	final protected void trace(String string) {
		logger.trace(stepName + ": " + string);
	}

	final protected void trace(String message, Throwable e) {
		logger.trace(stepName + ": " + message, e);
	}
}
