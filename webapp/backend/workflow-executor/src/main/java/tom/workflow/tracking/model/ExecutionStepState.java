package tom.workflow.tracking.model;

public class ExecutionStepState {

	private int numTasks;
	private int completedTasks;
	private int failedTasks;

	public int getNumTasks() {
		return numTasks;
	}

	public void setNumTasks(int numTasks) {
		this.numTasks = numTasks;
	}

	public int getCompletedTasks() {
		return completedTasks;
	}

	public void setCompletedTasks(int completedTasks) {
		this.completedTasks = completedTasks;
	}

	public int getFailedTasks() {
		return failedTasks;
	}

	public void setFailedTasks(int failedTasks) {
		this.failedTasks = failedTasks;
	}

	public void addTasks(int numTasksInStep) {
		numTasks += numTasksInStep;
	}

	public void completeTask() {
		completedTasks++;
	}

	public void failTask() {
		completeTask();
		failedTasks++;
	}

}
