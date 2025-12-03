package tom.task;

import java.util.List;

public interface MintyTask extends Runnable {

	// The results of this Task. This may be, but is not necessarily the same as the
	// output of runTask. This result represents all or a portion of the final
	// output of the overall workflow (for example, information retrieved from a
	// webpage during this Task).
	Packet getResult();

	// If any error occured, output it here to aid in debugging.
	String getError();

	// Returns true if this task will consume the packet if provided, false
	// otherwise.
	// This is to accommodate use cases such as grouping, where if a key does not
	// match the current group, we might not want the packet. Rejecting a packet
	// will cause the workflow to assume that this task does not want more packets
	// on the given input.
	default boolean wantsInput(int inputNum, Packet dataPacket) {
		return true;
	}

	// Provide a piece of input to the task. The task should return false if it does
	// not yet have all the input it needs for that input number, true if it does.
	boolean giveInput(int inputNum, Packet dataPacket);

	void setOutputConnectors(List<? extends OutputPort> outputs);

	// Return true if all inputs have all the required packets, false otherwise.
	boolean readyToRun();

	TaskSpec getSpecification();

	// This is called when there is no more input to available for this input.
	// (Upstream producer has completed).
	void inputTerminated(int i);

	boolean failed();

	void setLogger(TaskLogger workflowLogger);

	default String html() {
		return "";
	}
}
