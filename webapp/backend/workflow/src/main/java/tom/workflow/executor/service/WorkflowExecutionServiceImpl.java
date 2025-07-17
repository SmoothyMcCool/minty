package tom.workflow.executor.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.template.PugTemplate;
import tom.task.AiTask;

@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private final Logger logger = LogManager.getLogger(WorkflowExecutionServiceImpl.class);

    private final AsyncTaskExecutor taskExecutor;
    private final Map<String, TaskTracker> processTrackerMap;
    private final PugConfiguration pugConfiguration;

    @Value("${results.location}")
    private String resultsDir;

    @Value("${taskLibrary}")
    private String pugFileLocation;

    public WorkflowExecutionServiceImpl(@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            PugConfiguration pugConfiguration) {
        this.taskExecutor = taskExecutor;
        this.pugConfiguration = pugConfiguration;
        processTrackerMap = new HashMap<>();
    }

    @Override
    public String executeTask(AiTask task) {
        AiTaskWrapper wrappedTask = new AiTaskWrapper(task);
        wrappedTask.setExecutor(taskExecutor);

        trackTask(wrappedTask);
        taskExecutor.submit(wrappedTask);

        return task.taskName();
    }

    @Override
    public List<String> getAvailableResults() throws IOException {
        Path dir = Paths.get(resultsDir);
        List<String> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    results.add(entry.getFileName().toString());
                }
            }
        }
        return results;
    }

    @Override
    public String getResult(String taskName) throws IOException {
        Path dir = Paths.get(resultsDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (entry.getFileName().endsWith(taskName)) {
                    return Files.readString(entry);
                }
            }
        }
        return "Result not found.";
    }

    @Override
    public boolean deleteResult(String resultName) throws IOException {
        Path dir = Paths.get(resultsDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (entry.getFileName().endsWith(resultName)) {
                    Files.delete(entry);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void reportTaskComplete(TaskTracker tracker) {
        logger.info("reportTaskComplete: Task " + tracker.getTaskName() + " is complete.");
        Path location = Path.of(resultsDir + "/" + tracker.getTaskName());
        try {
            PugTemplate template = pugConfiguration.getTemplate(pugFileLocation + "/" + tracker.getTemplate());
            tracker.generateReport(location, template);
            processTrackerMap.remove(tracker.getTaskName());
        } catch (Exception e) {
            logger.error("reportTaskComplete: Caught exception " + e);
        }
    }

    private void trackTask(AiTaskWrapper task) {
        TaskTracker tracker = new TaskTracker(task, this);
        processTrackerMap.put(tracker.getTaskName(), tracker);
        task.setResultTracker(tracker);
    }
}
