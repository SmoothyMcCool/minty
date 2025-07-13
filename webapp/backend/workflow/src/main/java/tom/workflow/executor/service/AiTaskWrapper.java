package tom.workflow.executor.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.task.AiTask;

public class AiTaskWrapper implements Runnable {
	private final Logger logger = LogManager.getLogger(AiTaskWrapper.class);

	private TaskTracker tracker;
	private AsyncTaskExecutor taskExecutor;
	private boolean complete;
	private Instant startTime;
	private Instant endTime;
	private AiTask wrappedTask;
	private List<AiTaskWrapper> wrappedChildren;

	public AiTaskWrapper(AiTask wrappedTask) {
		this.wrappedTask = wrappedTask;
		wrappedChildren = null;
		startTime = Instant.MAX;
		endTime = Instant.MAX;

	}

	@Override
	public void run() {
		try {
			logger.info("Starting task name: " + wrappedTask.taskName());
			startTime = Instant.now();

			List<AiTask> children = wrappedTask.doWork();

			if (children != null && children.size() > 0) {
				wrappedChildren = new ArrayList<>();
				for (AiTask child : children) {
					AiTaskWrapper wrappedChild = new AiTaskWrapper(child);
					wrappedChildren.add(wrappedChild);
				}

				for (AiTaskWrapper child : wrappedChildren) {
					child.setResultTracker(tracker);
					child.setExecutor(taskExecutor);
					taskExecutor.submit(child);
				}
			}

			endTime = Instant.now();
			complete = true;
			tracker.taskComplete();
		}
		catch (Exception e) {
			logger.error("Caught exception while running task: " + e);
		}
	}

	public boolean isComplete() {
		boolean childrenComplete = true;
		if (wrappedChildren != null && !wrappedChildren.isEmpty()) {
			for (AiTaskWrapper child : wrappedChildren) {
				if (!child.isComplete()) {
					childrenComplete = false;
					break;
				}
			}
		}
		return complete && childrenComplete;
	}

	public void setResultTracker(TaskTracker tracker) {
		this.tracker = tracker;
	}

	public void setExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public Map<String, Object> getResult() {
		Map<String, Object> result = wrappedTask.getResult();

		if (wrappedChildren != null) {
			List<Map<String, Object>> childResults = new ArrayList<>();

			for (AiTaskWrapper child : wrappedChildren) {
				childResults.add(child.wrappedTask.getResult());
			}

			if (childResults.size() > 0) {
				result.put("subtasks", childResults);
			}
		}

		return result;
	}

	public String taskName() {
		return wrappedTask.taskName();
	}

	public String getResultTemplateFilename() {
		return wrappedTask.getResultTemplateFilename();
	}

}
