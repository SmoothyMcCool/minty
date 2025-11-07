package tom.workflow.tracking.model;

import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionStepState {

	private AtomicInteger numTasks;
	private AtomicInteger completedTasks;
	private AtomicInteger failedTasks;

	public ExecutionStepState() {
		numTasks = new AtomicInteger();
		completedTasks = new AtomicInteger();
		failedTasks = new AtomicInteger();
	}

	public int getNumTasks() {
		return numTasks.get();
	}

	public void setNumTasks(int numTasks) {
		this.numTasks.set(numTasks);
	}

	public int getCompletedTasks() {
		return completedTasks.get();
	}

	public void setCompletedTasks(int completedTasks) {
		this.completedTasks.set(completedTasks);
	}

	public int getFailedTasks() {
		return failedTasks.get();
	}

	public void setFailedTasks(int failedTasks) {
		this.failedTasks.set(failedTasks);
	}

	public void addTask() {
		numTasks.incrementAndGet();
	}

	public void addTasks(int numTasksInStep) {
		numTasks.getAndAdd(numTasksInStep);
	}

	public void completeTask() {
		completedTasks.getAndIncrement();
	}

	public void failTask() {
		failedTasks.getAndIncrement();
	}

}
