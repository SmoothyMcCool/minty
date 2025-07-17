package tom.workflow.executor.service;

import java.io.FileWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;

public class TaskTracker {

    private final Logger logger = LogManager.getLogger(TaskTracker.class);

    private final WorkflowExecutionService workflowExecutionService;
    private final Instant startTime;
    private Instant endTime;
    private final AiTaskWrapper initialTask;
    private boolean taskComplete;

    public TaskTracker(AiTaskWrapper initialTask, WorkflowExecutionService workflowExecutionService) {
        startTime = Instant.now();
        taskComplete = false;
        this.initialTask = initialTask;
        this.workflowExecutionService = workflowExecutionService;
    }

    public synchronized void taskComplete() {
        if (taskComplete) {
            return;
        }

        if (initialTask.isComplete()) {
            taskComplete = true;
            endTime = Instant.now();
            workflowExecutionService.reportTaskComplete(this);
        }

    }

    public String getTaskName() {
        return initialTask.taskName();
    }

    public String getTemplate() {
        return initialTask.getResultTemplateFilename();
    }

    public void generateReport(Path location, PugTemplate template) {

        try (FileWriter writer = new FileWriter(location.toString())) {
            Map<String, Object> results = initialTask.getResult();
            results.put("startTime", startTime);
            results.put("endTime", endTime);
            PugModel model = new PugModel(results);
            template.process(model, writer);
        } catch (Exception e) {
            logger.error("generateResult: Caught exception: " + e);
        }
    }

}
