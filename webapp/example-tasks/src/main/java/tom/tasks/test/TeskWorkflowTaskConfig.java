package tom.tasks.test;

import java.util.HashMap;
import java.util.Map;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class TeskWorkflowTaskConfig implements AiTaskConfig {

    private int delay;

    public TeskWorkflowTaskConfig() {
        delay = 0;
    }

    public TeskWorkflowTaskConfig(Map<String, String> config) {
        delay = Integer.parseInt(config.get("delay"));
    }

    @Override
    public Map<String, AiTaskConfigTypes> getConfig() {
        Map<String, AiTaskConfigTypes> cfg = new HashMap<>();
        cfg.put("delay", AiTaskConfigTypes.Number);
        return cfg;
    }

    public int getDelay() {
        return delay;
    }
}
