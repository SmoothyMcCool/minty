package tom.config.model;

import java.util.Map;

public record PluginConfig(String name, Map<String, Object> configuration) {

}
