package tom.config;

import java.util.Map;

import tom.config.model.MintyConfig;

public interface MintyConfiguration {

	Map<String, String> getSystemDefaults();

	Map<String, String> getUserDefaults();

	MintyConfig getConfig();

}
