package tom.api;

import java.util.Map;

public interface MintyProperties {

	boolean has(String key);

	String get(String key);

	String getOrDefault(String key, String defaultValue);

	int getInt(String key, int defaultValue);

	boolean getBoolean(String key, boolean defaultValue);

	Map<String, String> getSystemDefaults();

	Map<String, String> getUserDefaults();

}
