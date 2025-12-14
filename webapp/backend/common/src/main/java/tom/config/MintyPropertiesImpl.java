package tom.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import tom.api.MintyProperties;

public class MintyPropertiesImpl implements MintyProperties {

	private static final Logger logger = LogManager.getLogger(MintyProperties.class);

	private Properties properties = new Properties();
	private final Map<String, String> systemConfigs;
	private final Map<String, String> userConfigs;
	private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");

	public MintyPropertiesImpl() {
		systemConfigs = new HashMap<>();
		userConfigs = new HashMap<>();

		String path = System.getProperty("catalina.base") + "/conf/Minty/application.properties";
		try {
			FileSystemResource resource = new FileSystemResource(path);
			if (!resource.exists()) {
				throw new FileNotFoundException("External properties file not found at: " + path);
			}
			properties = PropertiesLoaderUtils.loadProperties(resource);

			logger.info("Loaded external properties from: " + path);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			properties.list(pw);
			logger.info("Properties:\n" + sw.toString());

		} catch (IOException e) {
			throw new RuntimeException("Failed to load external properties from: " + path, e);
		}
	}

	@Override
	public String get(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return placeholderHelper.replacePlaceholders(value, properties::getProperty);
	}

	@Override
	public String getOrDefault(String key, String defaultValue) {
		String value = get(key);
		return value != null ? value : defaultValue;
	}

	@Override
	public int getInt(String key, int defaultValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ignored) {
			}
		}
		return defaultValue;
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	@Override
	public boolean has(String key) {
		return properties.containsKey(key);
	}

	public Properties toMap() {
		Properties copy = new Properties();
		copy.putAll(properties);
		return copy;
	}

	public void addSystemConfigs(Map<String, String> map) {
		systemConfigs.putAll(map);
	}

	public void addUserConfigs(Map<String, String> map) {
		userConfigs.putAll(map);
	}

	@Override
	public Map<String, String> getSystemDefaults() {
		return systemConfigs;
	}

	@Override
	public Map<String, String> getUserDefaults() {
		return userConfigs;
	}

	public void readSystemDefaults() {
		systemConfigs.entrySet().forEach(entry -> {
			if (has(entry.getKey())) {
				logger.info(
						"Registering system default value " + entry.getKey() + "=" + properties.get(entry.getKey()));
				systemConfigs.put(entry.getKey(), get(entry.getKey()));
			} else {
				logger.warn("System default value not found! " + entry.getKey());
				systemConfigs.put(entry.getKey(), "Error! Default not defined in system properties!");
			}
		});
	}

}
