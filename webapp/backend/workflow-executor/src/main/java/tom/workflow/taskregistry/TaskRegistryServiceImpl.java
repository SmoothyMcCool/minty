package tom.workflow.taskregistry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import tom.task.MintyTask;
import tom.task.OutputTask;
import tom.task.OutputTaskSpec;
import tom.task.ServiceConsumer;
import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;
import tom.task.TaskSpec;
import tom.task.annotation.Output;
import tom.task.annotation.RunnableTask;
import tom.task.enumspec.EnumSpec;
import tom.task.enumspec.EnumSpecCreator;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;
import tom.workflow.model.OutputTaskSpecDescription;
import tom.workflow.model.TaskRequest;
import tom.workflow.model.TaskSpecDescription;

@Service
public class TaskRegistryServiceImpl implements TaskRegistryService {

	private final Logger logger = LogManager.getLogger(TaskRegistryServiceImpl.class);

	private final String taskLibrary;

	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> runnableTasks;
	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> outputTasks;
	private final List<Class<? extends EnumSpecCreator>> enumSpecCreators;
	private final Map<String, String> systemConfigs;
	private final Map<String, String> userConfigs;
	private final TaskServices taskServices;
	private final UserServiceInternal userService;
	private final ExternalProperties properties;

	public TaskRegistryServiceImpl(TaskServices taskServices, UserServiceInternal userService,
			ExternalProperties properties) {
		runnableTasks = new HashMap<>();
		outputTasks = new HashMap<>();
		enumSpecCreators = new ArrayList<>();
		systemConfigs = new HashMap<>();
		userConfigs = new HashMap<>();
		this.properties = properties;

		this.taskServices = taskServices;
		this.userService = userService;
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
				classes.addAll(getClassesFrom(path));
			}
			findAndRegisterAllTaskClasses(classes, paths);
			findAndRegisterAllEnumListClasses(classes, paths);

		} catch (Exception e) {
			logger.warn("initialize: failed to process libraries. ", e);
			throw new RuntimeException("Failed to load tasks.");
		}
	}

	private void findAndRegisterAllTaskClasses(List<String> classNames, List<Path> jarPaths)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, InvalidTaskException, IOException {
		List<URL> urls = new ArrayList<>();

		for (Path path : jarPaths) {
			urls.add(path.toUri().toURL());
		}

		try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
				MintyTask.class.getClassLoader())) {

			for (String className : classNames) {
				className = className.replace("/", ".");
				int suffixIdx = className.lastIndexOf(".");
				className = className.substring(0, suffixIdx);

				Class<?> loadedClass = classLoader.loadClass(className);

				if (isValidRunnableTask(loadedClass)) {
					loadRunnableTask(loadedClass);

				} else if (isValidOutputTask(loadedClass)) {
					loadOutputTask(loadedClass);
				}
			}

			// Now that we have processed all classes, let's try to read out our default
			// values
			readSystemDefaults();

		}

	}

	private void findAndRegisterAllEnumListClasses(List<String> classNames, List<Path> jarPaths) {
		List<URL> urls = new ArrayList<>();

		try {
			for (Path path : jarPaths) {
				urls.add(path.toUri().toURL());
			}
		} catch (MalformedURLException e) {
			logger.warn("findAndRegisterAllTaskClasses: Could not load URL. ", e);
		}

		try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
				EnumSpecCreator.class.getClassLoader())) {

			for (String className : classNames) {
				try {
					className = className.replace("/", ".");
					int suffixIdx = className.lastIndexOf(".");
					className = className.substring(0, suffixIdx);

					Class<?> loadedClass = classLoader.loadClass(className);

					if (EnumSpecCreator.class.isAssignableFrom(loadedClass)) {
						// Task must implement default constructor.
						if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
							throw new RuntimeException(
									"Class " + loadedClass.getName() + " does not implement default constructor.");
						}
						enumSpecCreators.add(loadedClass.asSubclass(EnumSpecCreator.class));
						logger.info("Registered EnumSpecCreator " + loadedClass.getName());

					}
				} catch (Exception e) {
					logger.warn("Failed to load EnumSpecCreator class: " + className, e);
				}
			}

		} catch (IOException | IllegalArgumentException | SecurityException e) {
			logger.warn("findAndRegisterAllTaskClasses: Failed to load class: ", e);
		}

	}

	private void loadOutputTask(Class<?> loadedClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		OutputTask bot = (OutputTask) loadedClass.getDeclaredConstructor().newInstance();

		OutputTaskSpec taskSpec = bot.getSpecification();
		String taskName = taskSpec.taskName();

		if (outputTasks.containsKey(taskName)) {
			Class<?> conflictedClass = outputTasks.get(taskName).left;
			logger.warn("Duplicate output task named " + taskName + " found implemented by "
					+ conflictedClass.toString() + " and " + loadedClass.toString());
			return;
		}

		TaskConfigSpec taskCfg = taskSpec.taskConfiguration();

		outputTasks.put(taskName, ImmutablePair.of(loadedClass, taskCfg.getConfig()));
		logger.info("Registering output task " + taskName);
	}

	private void loadRunnableTask(Class<?> loadedClass) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, InvalidTaskException {

		MintyTask mbt = (MintyTask) loadedClass.getDeclaredConstructor().newInstance();

		TaskSpec taskSpec = mbt.getSpecification();
		String taskName = taskSpec.taskName();

		if (runnableTasks.containsKey(taskName)) {
			Class<?> conflictedClass = runnableTasks.get(taskName).left;
			throw new InvalidTaskException("Duplicate task named " + taskName + " found implemented by "
					+ conflictedClass.toString() + " and " + loadedClass.toString());
		}

		TaskConfigSpec tcs = taskSpec.taskConfiguration();

		runnableTasks.put(taskName, ImmutablePair.of(loadedClass, tcs.getConfig()));

		List<String> taskSysCfgs = tcs.getSystemConfigVariables();
		systemConfigs.putAll(addConfigurationValues(tcs, taskName, tcs.getClass().getName(), taskSysCfgs));

		List<String> taskUserCfgs = tcs.getUserConfigVariables();
		userConfigs.putAll(addConfigurationValues(tcs, taskName, tcs.getClass().getName(), taskUserCfgs));

		logger.info("Registered task " + taskName);
	}

	private Map<String, String> addConfigurationValues(TaskConfigSpec taskCfg, String taskName, String configClassName,
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
									+ configClassName + "." + configKey);
				} else {
					result.put(taskName + "::" + configKey, "");
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

	private boolean implementsDefaultConstructor(Constructor<?>[] ctors) {
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterCount() == 0) {
				return true;
			}
		}

		return false;
	}

	private boolean implementsConfigurationClassConstructor(Constructor<?>[] ctors) {
		boolean foundConstructor = false;
		for (Constructor<?> ctor : ctors) {
			if (ctor.getParameterCount() == 1) {
				Class<?> paramType = ctor.getParameterTypes()[0];
				if (TaskConfigSpec.class.isAssignableFrom(paramType)) {
					foundConstructor = true;
					continue;
				}
			}
		}

		return foundConstructor;
	}

	private boolean isValidRunnableTask(Class<?> loadedClass) throws ClassNotFoundException {
		if (!MintyTask.class.isAssignableFrom(loadedClass) || !loadedClass.isAnnotationPresent(RunnableTask.class)) {
			return false;
		}

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		// Task must implement a constructor that takes an instance of a Configuration
		// class.
		if (!implementsConfigurationClassConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName()
					+ " does not have a constructor that takes a TaskConfigSpec as a parameter.");
			return false;
		}

		return true;
	}

	private boolean isValidOutputTask(Class<?> loadedClass) throws ClassNotFoundException {
		if (!OutputTask.class.isAssignableFrom(loadedClass) || !loadedClass.isAnnotationPresent(Output.class)) {
			return false;
		}

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		// Task must implement a constructor that takes an instance of a Configuration
		// class.
		if (!implementsConfigurationClassConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName()
					+ " does not have a constructor that takes a TaskConfigSpec as a parameter.");
			return false;
		}

		return true;
	}

	private List<String> getClassesFrom(Path path) {
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

	public MintyTask newTask(UserId userId, TaskRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request cannot be null.");
		}

		MintyTask task = null;

		if (!runnableTasks.containsKey(request.getTaskName())) {
			logger.warn("Task " + request.getTaskName() + " does not exist.");
			return task;
		}

		try {
			Class<?> taskClass = runnableTasks.get(request.getTaskName()).left;
			Constructor<?> ctor = taskClass.getDeclaredConstructor();

			MintyTask mt = (MintyTask) ctor.newInstance();

			User user = userService.getUserFromId(userId).get();
			List<String> sysCfgVars = mt.getSpecification().taskConfiguration().getSystemConfigVariables();
			List<String> userCfgVars = mt.getSpecification().taskConfiguration().getUserConfigVariables();

			for (String cfgVar : sysCfgVars) {
				request.getConfiguration().put(cfgVar, properties.get(request.getTaskName() + "::" + cfgVar));
			}
			for (String cfgVar : userCfgVars) {
				request.getConfiguration().put(cfgVar, user.getDefaults().get(request.getTaskName() + "::" + cfgVar));
			}

			TaskConfigSpec taskConfig = mt.getSpecification().taskConfiguration(request.getConfiguration());

			Constructor<?>[] constructors = taskClass.getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 1
						&& constructor.getParameterTypes()[0].isAssignableFrom(taskConfig.getClass())) {

					task = (MintyTask) constructor.newInstance(taskConfig);
				}
			}

			if (task instanceof ServiceConsumer) {
				((ServiceConsumer) task).setTaskServices(taskServices);
				((ServiceConsumer) task).setUserId(userId);
			}

		} catch (Exception e) {
			logger.warn("Failed to instantiate task for " + request.getTaskName() + ": ", e);
			task = null;
		}

		return task;
	}

	@Override
	public OutputTask newOutputTask(UserId userId, TaskRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("request cannot be null.");
		}

		OutputTask task = null;

		if (!outputTasks.containsKey(request.getTaskName())) {
			logger.warn("OutputTask " + request.getTaskName() + " does not exist.");
			return task;
		}

		try {
			Class<?> outputTaskClass = outputTasks.get(request.getTaskName()).left;

			task = (OutputTask) outputTaskClass.getDeclaredConstructor().newInstance();
			TaskConfigSpec taskConfig = task.getSpecification().taskConfiguration(request.getConfiguration());

			Constructor<?>[] constructors = outputTaskClass.getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 1
						&& constructor.getParameterTypes()[0].isAssignableFrom(taskConfig.getClass())) {

					task = (OutputTask) constructor.newInstance(taskConfig);
				}
			}

			if (task instanceof ServiceConsumer) {
				((ServiceConsumer) task).setTaskServices(taskServices);
				((ServiceConsumer) task).setUserId(userId);
			}

		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException e) {
			logger.warn("Failed to instantiate output task for " + request.getTaskName() + ": ", e);
			task = null;
		}

		return task;
	}

	@Override
	public Map<String, String> getConfigForTask(String taskName) {
		if (!runnableTasks.containsKey(taskName)) {
			logger.warn("Task " + taskName + " does not exist.");
			return Map.of();
		}

		Class<?> taskClass = runnableTasks.get(taskName).left;
		MintyTask mbt;

		try {

			mbt = (MintyTask) taskClass.getDeclaredConstructor().newInstance();
			TaskConfigSpec taskCfg = mbt.getSpecification().taskConfiguration();
			return configMapToStringMap(taskCfg.getConfig());

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			logger.warn("Failed to generate config for " + taskName + ": ", e);
			return Map.of();
		}

	}

	@Override
	public TaskSpec getSpecForTask(String taskName) {
		Class<?> taskClass = runnableTasks.get(taskName).left;

		try {
			MintyTask task = (MintyTask) taskClass.getDeclaredConstructor().newInstance();
			return task.getSpecification();

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	@Override
	public List<OutputTaskSpecDescription> getOutputTaskDescriptions() {
		Set<Entry<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>>> entrySet = outputTasks.entrySet();

		final List<OutputTaskSpecDescription> result = new ArrayList<>();

		entrySet.stream().forEach(entry -> {
			Class<?> clazz = entry.getValue().left;
			try {
				OutputTaskSpecDescription td = new OutputTaskSpecDescription();
				td.setTaskName(entry.getKey());
				td.setConfiguration(configMapToStringMap(entry.getValue().right));

				if (OutputTask.class.isAssignableFrom(clazz)) {
					OutputTask task = (OutputTask) clazz.getDeclaredConstructor().newInstance();

					td.setConfigSpec(task.getSpecification().taskConfiguration().getConfig());
					td.setSystemConfigVariables(task.getSpecification().taskConfiguration().getSystemConfigVariables());
					td.setUserConfigVariables(task.getSpecification().taskConfiguration().getUserConfigVariables());
				} else {
					throw new RuntimeException("Class " + clazz.getName() + " is not derived from MintyTask.");
				}

				result.add(td);

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logger.warn("Failed to instantiate " + clazz.getName() + " when generating task descriptions.", e);
			}
		});

		return result;
	}

	@Override
	public List<TaskSpecDescription> getTaskDescriptions() {
		Set<Entry<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>>> entrySet = runnableTasks.entrySet();

		final List<TaskSpecDescription> result = new ArrayList<>();

		entrySet.stream().forEach(entry -> {
			Class<?> clazz = entry.getValue().left;
			try {
				TaskSpecDescription td = new TaskSpecDescription();
				td.setTaskName(entry.getKey());
				td.setConfiguration(configMapToStringMap(entry.getValue().right));

				if (MintyTask.class.isAssignableFrom(clazz)) {
					MintyTask task = (MintyTask) clazz.getDeclaredConstructor().newInstance();

					td.setGroup(task.getSpecification().group());
					td.setExpects(task.getSpecification().expects());
					td.setProduces(task.getSpecification().produces());
					td.setNumInputs(task.getSpecification().numInputs());
					td.setNumOutputs(task.getSpecification().numOutputs());
					td.setConfigSpec(task.getSpecification().taskConfiguration().getConfig());
					td.setSystemConfigVariables(task.getSpecification().taskConfiguration().getSystemConfigVariables());
					td.setUserConfigVariables(task.getSpecification().taskConfiguration().getUserConfigVariables());
				} else {
					throw new RuntimeException("Class " + clazz.getName() + " is not derived from MintyTask.");
				}

				result.add(td);

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logger.warn("Failed to instantiate " + clazz.getName() + " when generating task descriptions.", e);
			}
		});

		return result;
	}

	@Override
	public Map<String, String> getConfigForOutputTask(String outputName) {
		if (!outputTasks.containsKey(outputName)) {
			logger.warn("Output task " + outputName + " does not exist.");
			return Map.of();
		}

		Class<?> taskClass = outputTasks.get(outputName).left;
		MintyTask mbt;

		try {
			mbt = (MintyTask) taskClass.getDeclaredConstructor().newInstance();
			TaskConfigSpec taskCfg = mbt.getSpecification().taskConfiguration();
			return configMapToStringMap(taskCfg.getConfig());

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			logger.warn("Could not generate output task configuration map for task " + outputName + ": ", e);
		}
		return Map.of();
	}

	@Override
	public Map<String, String> getSystemDefaults() {
		return systemConfigs;
	}

	@Override
	public Map<String, String> getUserDefaults() {
		return userConfigs;
	}

	private Map<String, String> configMapToStringMap(Map<String, TaskConfigTypes> configMap) {

		Map<String, String> taskCfg = new HashMap<>();

		if (configMap != null) {
			configMap.entrySet().stream().forEach(innerEntry -> {
				taskCfg.put(innerEntry.getKey(), innerEntry.getValue().toString());
			});
		}

		return taskCfg;
	}

	@Override
	public List<EnumSpec> getEnumerations(UserId userId) {
		List<EnumSpec> specs = new ArrayList<>();

		for (Class<? extends EnumSpecCreator> creatorClass : enumSpecCreators) {
			try {
				EnumSpecCreator creator = creatorClass.getDeclaredConstructor().newInstance();
				creator.setTaskServices(taskServices);
				specs.add(creator.getEnumList(userId));

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logger.warn("Could not generate EnumSpecCreator for " + creatorClass + ": ", e);
			}
		}

		return specs;
	}

}
