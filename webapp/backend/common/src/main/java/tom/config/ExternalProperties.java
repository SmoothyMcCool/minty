package tom.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.PropertyPlaceholderHelper;

public class ExternalProperties {

	private static final Logger logger = LogManager.getLogger(ExternalProperties.class);

	private Properties properties = new Properties();
	private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");

	public ExternalProperties() {
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

	/**
	 * Get a property value with ${...} references resolved.
	 */
	public String get(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return placeholderHelper.replacePlaceholders(value, properties::getProperty);
	}

	/**
	 * Get a property value with a default if missing.
	 */
	public String getOrDefault(String key, String defaultValue) {
		String value = get(key);
		return value != null ? value : defaultValue;
	}

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

	public boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	public boolean has(String key) {
		return properties.containsKey(key);
	}

	public Properties toMap() {
		return new Properties(properties);
	}
}
