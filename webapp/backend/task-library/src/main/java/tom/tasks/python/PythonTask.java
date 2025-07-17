package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicWorkflow;
import tom.task.services.TaskServices;

@PublicWorkflow(name = "Execute Python", configClass = "tom.tasks.python.PythonTaskConfig")
public class PythonTask implements AiTask, ServiceConsumer {

    private final Logger logger = LogManager.getLogger(PythonTask.class);

    private final PythonTaskConfig configuration;
    private Map<String, Object> result;
    private TaskServices taskServices;

    public PythonTask(PythonTaskConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public String taskName() {
        return "PythonTask-" + configuration.getPythonFile();
    }

    @Override
    public Map<String, Object> getResult() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        return map;
    }

    @Override
    public String getResultTemplateFilename() {
        return "default.pug";
    }

    @Override
    public List<AiTask> doWork() {
        logger.info("doWork: Executing " + configuration.getPythonFile());
        result = taskServices.getPythonService().execute(configuration.getPythonFile(),
                configuration.getInputDictionary());
        return null;
    }

    @Override
    public void setTaskServices(TaskServices taskServices) {
        this.taskServices = taskServices;
    }

}
