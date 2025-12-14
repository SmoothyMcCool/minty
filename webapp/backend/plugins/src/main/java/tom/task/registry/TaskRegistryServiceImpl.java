package tom.task.registry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.MintyProperties;
import tom.api.UserId;
import tom.api.model.ServiceConsumer;
import tom.api.services.TaskServices;
import tom.api.task.MintyTask;
import tom.api.task.OutputTask;
import tom.api.task.OutputTaskSpec;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.api.task.TaskSpec;
import tom.api.task.enumspec.EnumSpec;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.config.MintyPropertiesImpl;
import tom.task.model.OutputTaskSpecDescription;
import tom.task.model.TaskRequest;
import tom.task.model.TaskSpecDescription;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;

@Service
public class TaskRegistryServiceImpl implements TaskRegistryService {

	private final Logger logger = LogManager.getLogger(TaskRegistryServiceImpl.class);

	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> runnableTasks;
	private final Map<String, ImmutablePair<Class<?>, Map<String, TaskConfigTypes>>> outputTasks;
	private final Map<String, String> taskHelpFiles;
	private final Map<String, String> outputHelpFiles;
	private final List<Class<? extends EnumSpecCreator>> enumSpecCreators;
	private final TaskServices taskServices;
	private final UserServiceInternal userService;
	private final MintyPropertiesImpl properties;

	public TaskRegistryServiceImpl(TaskServices taskServices, UserServiceInternal userService,
			MintyProperties properties) {
		runnableTasks = new HashMap<>();
		outputTasks = new HashMap<>();
		taskHelpFiles = new HashMap<>();
		outputHelpFiles = new HashMap<>();
		enumSpecCreators = new ArrayList<>();
		this.properties = (MintyPropertiesImpl) properties;

		this.taskServices = taskServices;
		this.userService = userService;
	}

	@Override
	public void loadRunnableTask(Class<?> loadedClass) throws TaskLoadFailureException {

		try {
			MintyTask mbt = (MintyTask) loadedClass.getDeclaredConstructor().newInstance();

			TaskSpec taskSpec = mbt.getSpecification();
			String taskName = taskSpec.taskName();
			TaskConfigSpec tcs = taskSpec.taskConfiguration();

			if (runnableTasks.containsKey(taskName)) {
				Class<?> conflictedClass = runnableTasks.get(taskName).left;
				throw new RuntimeException("Duplicate output task named " + taskName + " found implemented by "
						+ conflictedClass.toString() + " and " + loadedClass.toString());
			}

			runnableTasks.put(taskName, ImmutablePair.of(loadedClass, tcs.getConfig()));

			List<String> taskSysCfgs = tcs.getSystemConfigVariables();
			properties.addSystemConfigs(addConfigurationValues(tcs, taskName, tcs.getClass().getName(), taskSysCfgs));

			List<String> taskUserCfgs = tcs.getUserConfigVariables();
			properties.addUserConfigs(addConfigurationValues(tcs, taskName, tcs.getClass().getName(), taskUserCfgs));

			logger.info("Registered task " + taskName);
		} catch (Exception e) {
			throw new TaskLoadFailureException(e);
		}
	}

	@Override
	public void loadOutputTask(Class<?> loadedClass) throws TaskLoadFailureException {

		try {
			OutputTask ot = (OutputTask) loadedClass.getDeclaredConstructor().newInstance();

			OutputTaskSpec taskSpec = ot.getSpecification();
			String taskName = taskSpec.taskName();
			TaskConfigSpec taskCfg = taskSpec.taskConfiguration();

			if (outputTasks.containsKey(taskName)) {
				Class<?> conflictedClass = outputTasks.get(taskName).left;
				throw new RuntimeException("Duplicate output task named " + taskName + " found implemented by "
						+ conflictedClass.toString() + " and " + loadedClass.toString());
			}

			outputTasks.put(taskName, ImmutablePair.of(loadedClass, taskCfg.getConfig()));
			logger.info("Registered output task " + taskName);
		} catch (Exception e) {
			throw new TaskLoadFailureException(e);
		}
	}

	@Override
	public void loadEnumSpecCreator(Class<?> loadedClass) {
		enumSpecCreators.add(loadedClass.asSubclass(EnumSpecCreator.class));
		logger.info("Registered EnumSpecCreator " + loadedClass.getName());
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
					result.put(configKey, "");
				}
			}
		}

		return result;
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

				if (MintyTask.class.isAssignableFrom(clazz)) {
					MintyTask task = (MintyTask) clazz.getDeclaredConstructor().newInstance();

					td.setGroup(task.getSpecification().group());
					td.setExpects(task.getSpecification().expects());
					td.setProduces(task.getSpecification().produces());
					td.setNumInputs(task.getSpecification().numInputs());
					td.setNumOutputs(task.getSpecification().numOutputs());
					td.setConfigSpec(task.getSpecification().taskConfiguration().getConfig());
					td.setConfiguration(task.getSpecification().taskConfiguration().getValues());
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
	public Map<String, String> getTaskHelpFiles() {
		return taskHelpFiles;
	}

	@Override
	public Map<String, String> getOutputHelpFiles() {
		return outputHelpFiles;
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
	public boolean hasTask(String fileName) {
		return taskHelpFiles.containsKey(fileName);
	}

	@Override
	public boolean hasOutputTask(String fileName) {
		return outputHelpFiles.containsKey(fileName);
	}

	@Override
	public void addTaskHelp(String fileName, String content) {
		if (!taskHelpFiles.containsKey(fileName)) {
			taskHelpFiles.put(fileName, content);
		}
	}

	@Override
	public void addOutputTaskHelp(String fileName, String content) {
		if (!outputHelpFiles.containsKey(fileName)) {
			outputHelpFiles.put(fileName, content);
		}
	}

	private Map<String, String> configMapToStringMap(Map<String, TaskConfigTypes> configMap) {

		Map<String, String> taskCfg = new HashMap<>();

		if (configMap != null) {
			configMap.entrySet().stream().forEach(innerEntry -> {
				TaskConfigTypes type = innerEntry.getValue();
				String value = switch (type) {
				case TaskConfigTypes.Boolean -> "false";
				case TaskConfigTypes.Number -> "";
				case TaskConfigTypes.String -> "";
				case TaskConfigTypes.StringList -> "[]";
				case TaskConfigTypes.Map -> "{}";
				case TaskConfigTypes.TextArea -> "";
				case TaskConfigTypes.EnumList -> "";
				case TaskConfigTypes.Packet -> {
					String result = "";
					try {
						result = new Packet().toJson();
					} catch (JsonProcessingException e) {
						logger.warn("Failed to JSON-ify packet: ", e);
					}
					yield result;
				}
				case TaskConfigTypes.Document -> "";
				};

				taskCfg.put(innerEntry.getKey(), value);
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
