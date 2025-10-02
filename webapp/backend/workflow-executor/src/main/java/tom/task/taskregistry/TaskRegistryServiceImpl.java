package tom.task.taskregistry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.api.UserId;
import tom.api.services.TaskServices;
import tom.config.ExternalProperties;
import tom.output.OutputTask;
import tom.output.annotations.Output;
import tom.output.noop.NullOutput;
import tom.task.MintyTask;
import tom.task.NullTask;
import tom.task.NullTaskConfig;
import tom.task.ServiceConsumer;
import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;
import tom.task.annotations.PublicTask;
import tom.workflow.model.TaskDescription;
import tom.workflow.model.TaskRequest;

@Service
public class TaskRegistryServiceImpl implements TaskRegistryService {

	private final Logger logger = LogManager.getLogger(TaskRegistryServiceImpl.class);

	private final String taskLibrary;

	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> publicTasks;
	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> publicOutputTasks;
	private final Map<String, String> systemConfigs;
	private final Map<String, String> userConfigs;
	private final TaskServices taskServices;
	private final ExternalProperties properties;
	private URLClassLoader classLoader;

	public TaskRegistryServiceImpl(TaskServices taskServices, ExternalProperties properties) {
		publicTasks = new HashMap<>();
		publicOutputTasks = new HashMap<>();
		systemConfigs = new HashMap<>();
		userConfigs = new HashMap<>();
		this.properties = properties;

		this.taskServices = taskServices;
		taskLibrary = properties.get("taskLibrary");
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for task libraries at " + taskLibrary);

		try (Stream<Path> stream = Files.list(Path.of(taskLibrary))) {
			List<Path> paths = stream.filter(file -> file.toString().endsWith(".jar")).toList();

			List<String> classes = new ArrayList<>();
			for (Path path : paths) {
				logger.info("Found library " + path.toString());
				classes.addAll(getTaskClassesFrom(path));
			}
			findAndRegisterAllTaskClasses(classes, paths);

		} catch (IOException e) {
			logger.warn("initialize: failed to process libraries. ", e);
		}
	}

	private void findAndRegisterAllTaskClasses(List<String> classNames, List<Path> jarPaths) {
		List<URL> urls = new ArrayList<>();

		try {
			for (Path path : jarPaths) {
				urls.add(path.toUri().toURL());
			}
		} catch (MalformedURLException e) {
			logger.warn("findAndRegisterAllTaskClasses: Could not load URL. ", e);
		}

		try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
				MintyTask.class.getClassLoader())) {

			// Cache the classloader for later.
			this.classLoader = classLoader;

			for (String className : classNames) {
				try {
					className = className.replace("/", ".");
					int suffixIdx = className.lastIndexOf(".");
					className = className.substring(0, suffixIdx);

					Class<?> loadedClass = classLoader.loadClass(className);

					if (isValidPublicTask(loadedClass)) {
						loadPublicTask(loadedClass);

					} else if (isValidOutputTask(loadedClass)) {
						loadOutputTask(loadedClass);
					}
				} catch (Exception e) {
					logger.warn("Failed to load class: " + className, e);
				}
			}

			// Now that we have processed all classes, let's try to read out our default
			// values
			readSystemDefaults();

		} catch (IOException | IllegalArgumentException | SecurityException e) {
			logger.warn("findAndRegisterAllTaskClasses: Failed to load class: ", e);
		}

	}

	private void loadOutputTask(Class<?> loadedClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		// Check if the annotated configuration class is valid.
		Output annotation = loadedClass.getAnnotation(Output.class);
		Class<?> configClass = classLoader.loadClass(annotation.configClass());

		if (!isConfigClassValid(configClass, loadedClass)) {
			return;
		}

		if (publicOutputTasks.containsKey(annotation.name())) {
			Class<?> conflictedClass = publicOutputTasks.get(annotation.name()).left;
			logger.warn("Duplicate output task named " + annotation.name() + " found implemented by "
					+ conflictedClass.toString() + " and " + loadedClass.toString());
			return;
		}

		TaskConfig taskCfg = (TaskConfig) configClass.getDeclaredConstructor().newInstance();

		publicOutputTasks.put(annotation.name(), ImmutablePair.of(loadedClass, taskCfg.getConfig()));
		logger.info("Registering output task " + annotation.name());
	}

	private void loadPublicTask(Class<?> loadedClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		TaskConfig taskCfg;
		// Check if the annotated configuration class is valid.
		PublicTask annotation = loadedClass.getAnnotation(PublicTask.class);
		Class<?> configClass = classLoader.loadClass(annotation.configClass());

		if (!isConfigClassValid(configClass, loadedClass)) {
			return;
		}

		if (publicTasks.containsKey(annotation.name())) {
			Class<?> conflictedClass = publicTasks.get(annotation.name()).left;
			logger.warn("Duplicate task named " + annotation.name() + " found implemented by "
					+ conflictedClass.toString() + " and " + loadedClass.toString());
			return;
		}

		taskCfg = (TaskConfig) configClass.getDeclaredConstructor().newInstance();

		publicTasks.put(annotation.name(), ImmutablePair.of(loadedClass, taskCfg.getConfig()));

		List<String> taskSysCfgs = taskCfg.getSystemConfigVariables();
		systemConfigs.putAll(addConfigurationValues(taskCfg, annotation, configClass, taskSysCfgs));

		List<String> taskUserCfgs = taskCfg.getUserConfigVariables();
		userConfigs.putAll(addConfigurationValues(taskCfg, annotation, configClass, taskUserCfgs));

		logger.info("Registered task " + annotation.name());
	}

	private Map<String, String> addConfigurationValues(TaskConfig taskCfg, PublicTask annotation, Class<?> configClass,
			List<String> taskConfigurationItems) {
		Map<String, String> result = new HashMap<>();

		if (taskConfigurationItems != null) {
			for (String configKey : taskConfigurationItems) {

				if (!taskCfg.getConfig().containsKey(configKey)) {
					logger.warn("Expected user config " + configKey + " not found in Config class "
							+ taskCfg.getClass().getName());
					continue;
				}

				if (taskCfg.getConfig().get(configKey).compareTo(TaskConfigTypes.String) != 0
						&& taskCfg.getConfig().get(configKey).compareTo(TaskConfigTypes.Number) != 0
						&& taskCfg.getConfig().get(configKey).compareTo(TaskConfigTypes.Boolean) != 0) {
					logger.warn(
							"Cannot declare non-primitive (or String) types as system or user configuration items. Item is "
									+ configClass.getName() + "." + configKey);
				} else {
					result.put(annotation.name() + "::" + configKey, "");
				}
			}
		}

		return result;
	}

	private void readSystemDefaults() {
		systemConfigs.entrySet().forEach(entry -> {
			if (properties.has(entry.getKey())) {
				logger.info(
						"Registering system default value " + entry.getKey() + "=" + properties.get(entry.getKey()));
				systemConfigs.put(entry.getKey(), properties.get(entry.getKey()));
			} else {
				logger.warn("System default value not found! " + entry.getKey());
				systemConfigs.put(entry.getKey(), "Error! Default not defined in system properties!");
			}
		});
	}

	private boolean isConfigClassValid(Class<?> configClass, Class<?> loadedClass) {
		Constructor<?>[] ctors = configClass.getDeclaredConstructors();

		// The config class must implement a default constructor, and a constructor that
		// takes a Map<String, String>
		if (!implementsDefaultConstructor(ctors)) {
			logger.warn("Found Public Task " + loadedClass.getName()
					+ " declares an TaskConfig class that does not implement a default constructor.");
			return false;
		}

		// The config class must implement a constructor that takes a Map<String,
		// String>
		if (!implementsMapConstructor(ctors)) {
			logger.warn("Found Public Task " + loadedClass.getName()
					+ " declares an TaskConfig class that does not implement a constructor that takes a Map<String, String> parameter.");
			return false;
		}

		// If a config class is declared, the Task must implement a constructor
		// that takes an instance of the specific class from the PublicTask
		// annotation.
		if (!configClass.equals(NullTaskConfig.class)
				&& !implementsConfigConstructor(loadedClass.getDeclaredConstructors(), configClass)) {
			logger.warn("Found Public Task " + loadedClass.getName()
					+ " does not implement a constructor that takes an instance of TaskConfig or derived class.");
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

	private boolean implementsMapConstructor(Constructor<?>[] ctors) {
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterCount() == 1) {
				Type[] params = ctor.getGenericParameterTypes();
				if (params != null) {
					Type param = params[0];

					if (param instanceof ParameterizedType) {
						ParameterizedType type = (ParameterizedType) param;

						if (type.getRawType().equals(Map.class)) {
							Type[] mapTypes = type.getActualTypeArguments();

							if (mapTypes.length == 2 && mapTypes[0].equals(String.class)
									&& mapTypes[1].equals(String.class)) {
								return true;
							}
						}
					}
				}
			}
		}

		return false;
	}

	private boolean implementsConfigConstructor(Constructor<?>[] ctors, Class<?> configClass) {
		boolean foundConstructor = false;
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterCount() == 1) {
				if (ctor.getParameters()[0].getType().equals(configClass)) {
					foundConstructor = true;
					continue;
				}
			}
		}

		return foundConstructor;
	}

	private boolean isValidPublicTask(Class<?> loadedClass) throws ClassNotFoundException {
		if (!MintyTask.class.isAssignableFrom(loadedClass) || !loadedClass.isAnnotationPresent(PublicTask.class)) {
			return false;
		}

		PublicTask annotation = loadedClass.getAnnotation(PublicTask.class);

		Class<?> configClass = classLoader.loadClass(annotation.configClass());

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		// Config class must implement the TaskConfig interface
		if (!TaskConfig.class.isAssignableFrom(configClass)) {
			logger.warn("Class " + loadedClass.getName() + " annotated configClass does not implement "
					+ TaskConfig.class.getName());
			return false;
		}

		return true;
	}

	private boolean isValidOutputTask(Class<?> loadedClass) throws ClassNotFoundException {
		if (!OutputTask.class.isAssignableFrom(loadedClass) || !loadedClass.isAnnotationPresent(Output.class)) {
			return false;
		}

		Output annotation = loadedClass.getAnnotation(Output.class);

		Class<?> configClass = classLoader.loadClass(annotation.configClass());

		// Class must implement the TaskConfig interface
		if (!TaskConfig.class.isAssignableFrom(configClass)) {
			logger.warn("Class " + loadedClass.getName() + " annotated configClass does not implement "
					+ TaskConfig.class.getName());
			return false;
		}

		return true;
	}

	private List<String> getTaskClassesFrom(Path path) {
		List<String> classes = new ArrayList<>();

		try (JarFile jarFile = new JarFile(path.toFile())) {
			var entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class") && !entry.isDirectory()) {
					classes.add(name);
				}
			}
		} catch (IOException e) {
			logger.warn("getTaskClassesFrom: caught exception: ", e);
		}
		return classes;
	}

	@Override
	public MintyTask newTask(UserId userId, TaskRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("request cannot be null.");
		}

		MintyTask task = new NullTask();

		if (!publicTasks.containsKey(request.getName())) {
			logger.warn("Task " + request.getName() + " does not exist.");
			return task;
		}

		try {
			Class<?> taskClass = publicTasks.get(request.getName()).left;

			PublicTask annotation = taskClass.getAnnotation(PublicTask.class);
			Class<?> configClass = classLoader.loadClass(annotation.configClass());

			// First instantiate the config class with data.

			TaskConfig taskConfig = new NullTaskConfig();

			// If this request contains no config data, then just call the default
			// constructor
			if (request.getConfiguration().isEmpty()) {
				Constructor<?> constructor = configClass.getDeclaredConstructor();
				taskConfig = (TaskConfig) constructor.newInstance();
			} else {
				Constructor<?>[] constructors = configClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1
							&& constructor.getParameterTypes()[0].isAssignableFrom(Map.class)) {
						taskConfig = (TaskConfig) constructor.newInstance(request.getConfiguration());
						break;
					}
				}
			}

			// Now instantiate the task, passing it the config object.

			// If the configClass is the NullTaskConfig class, then just call the default
			// constructor
			if (configClass.equals(NullTaskConfig.class)) {
				Constructor<?> constructor = taskClass.getDeclaredConstructor();
				task = (MintyTask) constructor.newInstance();
			} else {
				Constructor<?>[] constructors = taskClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1
							&& constructor.getParameterTypes()[0].isAssignableFrom(configClass)) {

						task = (MintyTask) constructor.newInstance(configClass.cast(taskConfig));
					}
				}
			}

			if (task instanceof ServiceConsumer) {
				((ServiceConsumer) task).setTaskServices(taskServices);
				((ServiceConsumer) task).setUserId(userId);
			}

		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.warn("Failed to instantiate task for " + request.getName() + ": ", e);
			task = null;
		} catch (ClassNotFoundException e) {
			logger.warn("Failed to instantiate task for " + request.getName() + ". Class not found: ", e);
		} catch (NoSuchMethodException e) {
			logger.warn("Failed to instantiate class: ", e);
		}

		return task;

	}

	@Override
	public OutputTask newOutputTask(UserId userId, TaskRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("request cannot be null.");
		}

		OutputTask task = new NullOutput();

		if (!publicOutputTasks.containsKey(request.getName())) {
			logger.warn("OutputTask " + request.getName() + " does not exist.");
			return task;
		}

		try {
			Class<?> outputTaskClass = publicOutputTasks.get(request.getName()).left;

			Output annotation = outputTaskClass.getAnnotation(Output.class);
			Class<?> configClass = classLoader.loadClass(annotation.configClass());

			// First instantiate the config class with data.
			TaskConfig taskConfig = new NullTaskConfig();

			// If this out config contains no data, call the default constructor.
			if (request.getConfiguration().isEmpty()) {
				Constructor<?> constructor = configClass.getDeclaredConstructor();
				taskConfig = (TaskConfig) constructor.newInstance();
			} else {
				Constructor<?>[] constructors = configClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1
							&& constructor.getParameterTypes()[0].isAssignableFrom(Map.class)) {
						taskConfig = (TaskConfig) constructor.newInstance(request.getConfiguration());
						break;
					}
				}
			}

			// Now instantiate the output task, passing it the config object.

			// If the configClass is the NullTaskConfig class, then just call the default
			// constructor
			if (configClass.equals(NullTaskConfig.class)) {
				Constructor<?> constructor = outputTaskClass.getDeclaredConstructor();
				task = (OutputTask) constructor.newInstance();
			} else {
				Constructor<?>[] constructors = outputTaskClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1
							&& constructor.getParameterTypes()[0].isAssignableFrom(configClass)) {

						task = (OutputTask) constructor.newInstance(configClass.cast(taskConfig));
					}
				}
			}

			if (task instanceof ServiceConsumer) {
				((ServiceConsumer) task).setTaskServices(taskServices);
				((ServiceConsumer) task).setUserId(userId);
			}

		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.warn("Failed to instantiate task for " + request.getName() + ": ", e);
			task = null;
		} catch (ClassNotFoundException e) {
			logger.warn("Failed to instantiate task for " + request.getName() + ". Class not found: ", e);
		} catch (NoSuchMethodException e) {
			logger.warn("Failed to instantiate class: ", e);
		}

		return task;
	}

	@Override
	public List<TaskDescription> getTasks() {
		return getTaskDescriptions(publicTasks);
	}

	@Override
	public Map<String, String> getConfigForTask(String taskName) {
		if (!publicTasks.containsKey(taskName)) {
			logger.warn("Task " + taskName + " does not exist.");
			return Map.of();
		}

		Class<?> taskClass = publicTasks.get(taskName).left;
		PublicTask annotation = taskClass.getDeclaredAnnotation(PublicTask.class);

		try {
			Class<?> configClass = classLoader.loadClass(annotation.configClass());
			TaskConfig taskCfg = (TaskConfig) configClass.getDeclaredConstructor().newInstance();
			return configMapToStringMap(taskCfg.getConfig());

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.warn("getConfigForTask: Could not instantiate instance of " + annotation.configClass() + ": ", e);
			return Map.of();
		}
	}

	@Override
	public List<TaskDescription> getOutputTaskTemplates() {
		return getTaskDescriptions(publicOutputTasks);
	}

	private List<TaskDescription> getTaskDescriptions(
			Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> taskMap) {
		Set<Entry<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>>> entrySet = taskMap.entrySet();

		final List<TaskDescription> result = new ArrayList<>();

		entrySet.stream().forEach(entry -> {
			Class<?> clazz = entry.getValue().left;
			try {
				TaskDescription td = new TaskDescription();
				td.setName(entry.getKey());
				td.setConfiguration(configMapToStringMap(entry.getValue().right));

				if (MintyTask.class.isAssignableFrom(clazz)) {
					MintyTask task = (MintyTask) clazz.getDeclaredConstructor().newInstance();

					td.setInputs(task.expects());
					td.setOutputs(task.produces());
				} else {
					td.setInputs("");
					td.setOutputs("");
				}

				result.add(td);

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logger.warn("Failed to instantiate " + clazz.getName() + " when generating task descriptions.");
			}
		});

		return result;
	}

	@Override
	public Map<String, String> getConfigForOutputTask(String outputName) {
		if (!publicOutputTasks.containsKey(outputName)) {
			logger.warn("Output task " + outputName + " does not exist.");
			return Map.of();
		}

		Class<?> taskClass = publicOutputTasks.get(outputName).left;
		Output annotation = taskClass.getDeclaredAnnotation(Output.class);

		try {
			Class<?> configClass = classLoader.loadClass(annotation.configClass());
			TaskConfig taskCfg = (TaskConfig) configClass.getDeclaredConstructor().newInstance();
			return configMapToStringMap(taskCfg.getConfig());

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.warn("getConfigForRenderer: Could not instantiate instance of " + annotation.configClass() + ": ",
					e);
			return Map.of();
		}
	}

	@Override
	public Map<String, String> getSystemDefaults() {
		return systemConfigs;
	}

	@Override
	public Map<String, String> getUserDefaults() {
		return userConfigs;
	}

	private Map<String, String> configMapToStringMap(Map<String, TaskConfigTypes> aiConfigMap) {

		Map<String, String> taskCfg = new HashMap<>();

		if (aiConfigMap != null) {
			aiConfigMap.entrySet().stream().forEach(innerEntry -> {
				taskCfg.put(innerEntry.getKey(), innerEntry.getValue().toString());
			});
		}

		return taskCfg;
	}

}
