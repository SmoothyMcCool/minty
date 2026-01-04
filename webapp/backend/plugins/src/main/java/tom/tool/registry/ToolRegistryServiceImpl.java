package tom.tool.registry;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import tom.api.UserId;
import tom.api.model.services.ConfigurationConsumer;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.config.MintyConfiguration;
import tom.config.MintyConfigurationImpl;
import tom.tool.model.MintyToolDescription;

@Service
public class ToolRegistryServiceImpl implements ToolRegistryService {

	private final Logger logger = LogManager.getLogger(ToolRegistryServiceImpl.class);

	private final Map<String, Class<?>> tools;
	private final List<MintyToolDescription> descriptions;
	private final PluginServices pluginServices;
	private final MintyConfigurationImpl configuration;

	public ToolRegistryServiceImpl(PluginServices pluginServices, MintyConfiguration configuration) {
		tools = new HashMap<>();
		descriptions = new ArrayList<>();
		this.pluginServices = pluginServices;
		this.configuration = (MintyConfigurationImpl) configuration;
	}

	@Override
	public Object getTool(String toolName, UserId userId) {
		if (!tools.containsKey(toolName)) {
			return null;
		}

		try {

			Class<?> clazz = tools.get(toolName);
			MintyTool o = (MintyTool) clazz.getDeclaredConstructor().newInstance();
			if (o instanceof ServiceConsumer) {
				((ServiceConsumer) o).setPluginServices(pluginServices);
				((ServiceConsumer) o).setUserId(userId);
			}
			if (o instanceof ConfigurationConsumer) {
				((ConfigurationConsumer) o).setProperties(configuration.getSystemDefaults(),
						configuration.getUserDefaults());
			}
			return o;

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			logger.warn("Failed to instantiate instance of tool " + toolName);
			return null;
		}
	}

	@Override
	public List<MintyToolDescription> listTools() {
		return descriptions;
	}

	@Override
	public void loadTool(Class<?> loadedClass) throws ToolLoadFailureException {

		try {
			MintyTool mt = (MintyTool) loadedClass.getDeclaredConstructor().newInstance();

			String toolName = mt.name();

			if (tools.containsKey(toolName)) {
				Object conflictedClass = tools.get(toolName);
				throw new ToolLoadFailureException("Duplicate tool named " + toolName + " found implemented by "
						+ conflictedClass.toString() + " and " + loadedClass.toString());
			}

			tools.put(toolName, mt.getClass());
			descriptions.add(new MintyToolDescription(mt.name(), mt.description()));

			logger.info("Registered tool " + toolName);
		} catch (Exception e) {
			throw new ToolLoadFailureException(e);
		}
	}

}
