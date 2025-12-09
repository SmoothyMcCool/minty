package tom.tools.toolregistry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.api.UserId;
import tom.api.services.TaskServices;
import tom.config.ExternalProperties;
import tom.task.ConfigurationConsumer;
import tom.task.ServiceConsumer;
import tom.tool.MintyTool;
import tom.tools.model.MintyToolDescription;

@Service
public class ToolRegistryServiceImpl implements ToolRegistryService {

	private final Logger logger = LogManager.getLogger(ToolRegistryServiceImpl.class);

	private final String toolLibrary;
	private final Map<String, Class<?>> tools;
	private final List<MintyToolDescription> descriptions;
	private final TaskServices taskServices;
	private final ExternalProperties properties;

	public ToolRegistryServiceImpl(TaskServices taskServices, ExternalProperties properties) {
		tools = new HashMap<>();
		descriptions = new ArrayList<>();
		toolLibrary = properties.get("toolLibrary");
		this.taskServices = taskServices;
		this.properties = properties;
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for tool libraries at " + toolLibrary);

		try (Stream<Path> stream = Files.list(Path.of(toolLibrary))) {
			List<Path> paths = stream.filter(file -> file.toString().endsWith(".jar")).toList();

			List<String> classes = new ArrayList<>();
			for (Path path : paths) {
				logger.info("Found library " + path.toString());
				classes.addAll(getFilesFrom(path, ".class"));
			}
			findAndRegisterAllToolClasses(classes, paths);

		} catch (Exception e) {
			logger.warn("initialize: failed to process libraries. ", e);
			throw new RuntimeException("Failed to load tools.");
		}
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
				((ServiceConsumer) o).setTaskServices(taskServices);
			}
			if (o instanceof ConfigurationConsumer) {
				((ConfigurationConsumer) o).setProperties(properties.toMap());
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

	private void findAndRegisterAllToolClasses(List<String> classNames, List<Path> jarPaths)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, DuplicateToolException {
		List<URL> urls = new ArrayList<>();

		for (Path path : jarPaths) {
			urls.add(path.toUri().toURL());
		}

		try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
				Tool.class.getClassLoader())) {

			for (String className : classNames) {
				className = className.replace("/", ".");
				int suffixIdx = className.lastIndexOf(".");
				className = className.substring(0, suffixIdx);

				Class<?> loadedClass = classLoader.loadClass(className);

				if (isValidTool(loadedClass)) {
					loadTool(loadedClass);
				}
			}
		}

	}

	private void loadTool(Class<?> loadedClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, DuplicateToolException {

		MintyTool mt = (MintyTool) loadedClass.getDeclaredConstructor().newInstance();

		String taskName = mt.name();

		if (tools.containsKey(taskName)) {
			Object conflictedClass = tools.get(taskName);
			throw new DuplicateToolException("Duplicate task named " + taskName + " found implemented by "
					+ conflictedClass.toString() + " and " + loadedClass.toString());
		}

		tools.put(taskName, mt.getClass());
		descriptions.add(new MintyToolDescription(mt.name(), mt.description()));

		logger.info("Registered task " + taskName);
	}

	private boolean isValidTool(Class<?> loadedClass) throws ClassNotFoundException {
		if (!MintyTool.class.isAssignableFrom(loadedClass)) {
			return false;
		}

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		// Task must have a method tagged with the @Tool annotation.
		boolean found = false;
		for (Method m : loadedClass.getDeclaredMethods()) {
			if (m.isAnnotationPresent(Tool.class)) {
				found = true;
			}
		}
		if (!found) {
			return false;
		}

		return true;
	}

	private boolean implementsDefaultConstructor(Constructor<?>[] ctors) {
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterCount() == 0) {
				return true;
			}
		}

		return false;
	}

	private List<String> getFilesFrom(Path path, String extension) {
		List<String> files = new ArrayList<>();

		try (JarFile jarFile = new JarFile(path.toFile())) {
			var entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(extension) && !entry.isDirectory()) {
					files.add(name);
				}
			}
		} catch (IOException e) {
			logger.warn("getFilesFrom: caught exception: ", e);
		}
		return files;
	}

}
