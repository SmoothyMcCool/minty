package tom.tasks.aiassistant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicWorkflow;
import tom.task.model.AssistantQuery;
import tom.task.services.TaskServices;

@PublicWorkflow(name = "Ask Assistant", configClass = "tom.tasks.aiassistant.AiAssistantTaskConfig")
public class AiAssistantTask implements AiTask, ServiceConsumer {

    private TaskServices taskServices;
    private UUID uuid;
    private String result;
    private AiAssistantTaskConfig config;
    private int userId;

    public AiAssistantTask(AiAssistantTaskConfig data) {
        config = data;
    }

    @Override
    public void setTaskServices(TaskServices taskServices) {
        this.taskServices = taskServices;
        uuid = UUID.randomUUID();
    }

    @Override
    public String taskName() {
        return "EvaluatePlan-" + uuid;
    }

    @Override
    public Map<String, Object> getResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("result", this.result);
        return result;
    }

    @Override
    public String getResultTemplateFilename() {
        return "default.pug";
    }

    @Override
    public List<AiTask> doWork() {
        AssistantQuery query = new AssistantQuery();
        query.setAssistantId(config.getAssistant());
        query.setQuery(config.getPrompt());

        result = taskServices.getAssistantService().ask(userId, query);
        return null;
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
    }

}
