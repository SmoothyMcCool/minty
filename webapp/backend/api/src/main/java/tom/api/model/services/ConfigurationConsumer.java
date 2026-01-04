package tom.api.model.services;

import java.util.Map;

public interface ConfigurationConsumer {

	void setProperties(Map<String, String> systemProperties, Map<String, String> userProperties);

}
