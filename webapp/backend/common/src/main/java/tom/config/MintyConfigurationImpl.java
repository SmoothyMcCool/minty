package tom.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import tom.config.model.MintyConfig;

public class MintyConfigurationImpl implements MintyConfiguration {

	private static final Logger logger = LogManager.getLogger(MintyConfigurationImpl.class);

	private final MintyConfig config;
	private final Map<String, String> systemConfigs;
	private final Map<String, String> userConfigs;
	private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");

	public MintyConfigurationImpl() throws StreamReadException, DatabindException, IOException {
		systemConfigs = new HashMap<>();
		userConfigs = new HashMap<>();

		String path = System.getProperty("catalina.base") + "/conf/Minty/application.yaml";

		Resource resource = new FileSystemResource(path);
		if (!resource.exists()) {
			throw new RuntimeException("Properties file not found at: " + path);
		}

		// First load as flat properties to resolve placeholders.
		YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
		yamlFactory.setResources(resource);
		yamlFactory.afterPropertiesSet();

		Properties rawProps = yamlFactory.getObject();
		Properties resolvedProps = new Properties();

		if (rawProps == null) {
			throw new NullPointerException("Failed to read raw properties from application.yaml");
		}

		for (String name : rawProps.stringPropertyNames()) {
			String raw = rawProps.getProperty(name);
			String resolved = placeholderHelper.replacePlaceholders(raw, rawProps::getProperty);
			resolvedProps.setProperty(name, resolved);
		}

		// Load YAML as a structured object
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.registerModule(new JavaTimeModule());
		Map<String, Object> tree = mapper.readValue(resource.getInputStream(), new TypeReference<>() {
		});

		// Apply resolved values onto the tree.
		Object resolvedTree = resolveTree(tree, "", resolvedProps);
		config = mapper.convertValue(resolvedTree, MintyConfig.class);

		logger.info("Loaded external YAML from: " + path);
		logger.info("Properties:\n" + config.prettyPrint());
	}

	@Override
	public MintyConfig getConfig() {
		return config;
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

	public void validateSystemDefaults() {
		// This function should be run after all system defaults discovered while
		// loading plugins are added. It compares the defaults that are expected against
		// those that are available.
		Map<String, String> updated = new HashMap<>();

		for (Entry<String, String> entry : systemConfigs.entrySet()) {
			String key = entry.getKey();

			if (config.systemDefaults().containsKey(key)) {
				String value = config.systemDefaults().get(key).toString();
				logger.info("Registering system default value " + key + "=" + value);
				updated.put(key, value);
			} else {
				logger.warn("System default value not found! " + key);
				updated.put(key, "Error! Default not defined in system properties!");
				throw new PropertyNotFoundException("System default value not found: " + key);
			}
		}

		systemConfigs.clear();
		systemConfigs.putAll(updated);
	}

	private static Object resolveTree(Object node, String path, Properties resolved) {
		if (node instanceof Map<?, ?> map) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (var e : map.entrySet()) {
				String key = e.getKey().toString();
				String newPath = path.isEmpty() ? key : path + "." + key;
				out.put(key, resolveTree(e.getValue(), newPath, resolved));
			}
			return out;
		}
		if (node instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) {
				out.add(resolveTree(list.get(i), path + "." + i, resolved));
			}
			return out;
		}
		if (node instanceof String) {
			return resolved.getProperty(path, node.toString());
		}
		return node;
	}
}
