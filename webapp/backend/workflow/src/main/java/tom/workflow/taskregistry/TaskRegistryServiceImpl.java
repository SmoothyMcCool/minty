package tom.workflow.taskregistry;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.task.AiTask;
import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;
import tom.task.NullTaskConfig;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicWorkflow;
import tom.task.services.TaskServices;
import tom.workflow.controller.WorkflowRequest;

@Service
public class TaskRegistryServiceImpl implements TaskRegistryService {

	private final Logger logger = LogManager.getLogger(TaskRegistryServiceImpl.class);

	@Value("${taskLibrary}")
	private String taskLibrary;

	private Map<String, ImmutablePair<Class<?>, Map<String, AiTaskConfigTypes>>> publicWorkflows;
	private final TaskServices taskServices;
	private URLClassLoader classLoader;

	public TaskRegistryServiceImpl(TaskServices taskServices) {
		publicWorkflows = new HashMap<>();
		this.taskServices = taskServices;
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for task libraries at " + taskLibrary);

		try (Stream<Path> stream = Files.list(Path.of(taskLibrary))) {
			List<Path> paths = Files.list(Path.of(taskLibrary))
				.filter(file -> file.toString().endsWith(".jar"))
				.toList();

			List<String> classes = new ArrayList<>();
			for (Path path : paths) {
				logger.info("Found library " + path.toString());
				classes.addAll(getTaskClassesFrom(path));
			}
			findAndRegisterAllTaskClasses(classes, paths);

		} catch (IOException e) {
			logger.warn("initialize: failed to process libraries. " + e);
		}
	}

	private void findAndRegisterAllTaskClasses(List<String> classNames, List<Path> jarPaths) {
		List<URL> urls = new ArrayList<>();

		try {
			for (Path path : jarPaths) {
				urls.add(path.toUri().toURL());
			}
		}
		catch (MalformedURLException e) {
			logger.warn("findAndRegisterAllTaskClasses: Could not load URL. " + e);
		}

		try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), AiTask.class.getClassLoader())) {
			for (String className : classNames) {
				className = className.replace("/", ".");
				int suffixIdx = className.lastIndexOf(".");
				className = className.substring(0, suffixIdx);

				Class<?> loadedClass = classLoader.loadClass(className);

				AiTaskConfig taskCfg = new NullTaskConfig();
				if (isValidPublicWorkflow(loadedClass, classLoader)) {

					// Check if the annotated configuration class is valid.
					PublicWorkflow annotation = loadedClass.getAnnotation(PublicWorkflow.class);
					Class<?> configClass = classLoader.loadClass(annotation.configClass());

					Constructor<?>[] ctors = configClass.getDeclaredConstructors();

					// The config class must implement a default constructor, and a constructor that takes a Map<String, String>
					if (!implementsDefaultConstructor(ctors)) {
						logger.warn("Found Public Workflow " + loadedClass.getName() + " declares an AiTaskConfig class that does not implement a default constructor.");
						continue;
					}

					// The config class must implement a constructor that takes a Map<String, String>
					if (!implementsMapConstructor(ctors)) {
						logger.warn("Found Public Workflow " + loadedClass.getName() + " declares an AiTaskConfig class that does not implement a constructor that takes a Map<String, String> parameter.");
						continue;
					}

					// If a config class is declared, the Workflow task must implement a constructor that takes an instance of the specific class from the PublicWorkflow annotation.
					if (!configClass.equals(NullTaskConfig.class) && !implementsConfigConstructor(loadedClass.getDeclaredConstructors(), configClass)) {
						logger.warn("Found Public Workflow " + loadedClass.getName() + " does not implement a constructor that takes an instance of AiTaskConfig or derived class.");
						continue;
					}

					taskCfg = (AiTaskConfig) configClass.getDeclaredConstructor().newInstance();

					if (publicWorkflows.containsKey(annotation.name())) {
						Class<?> conflictedClass = publicWorkflows.get(annotation.name()).left;
						logger.warn("Duplicate workflow named " + annotation.name() + " found implemented by " + conflictedClass.toString() + " and " + loadedClass.toString());
						continue;
					}
	
					publicWorkflows.put(annotation.name(), ImmutablePair.of(loadedClass, taskCfg.getConfig()));
					logger.info("Registering workflow " + annotation.name());
				}
			}

			// Cache the classloader for later.
			this.classLoader = classLoader;

		} catch (IOException |
				ClassNotFoundException |
				InstantiationException |
				IllegalAccessException |
				IllegalArgumentException |
				InvocationTargetException |
				NoSuchMethodException |
				SecurityException e) {
			logger.warn("findAndRegisterAllTaskClasses: Failed to load class: " + e);
		}

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

							if (mapTypes.length ==2 &&
									mapTypes[0].equals(String.class)&&
									mapTypes[1].equals(String.class)) {
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

	private boolean isValidPublicWorkflow(Class<?> loadedClass, URLClassLoader classLoader) throws ClassNotFoundException {
		if (!AiTask.class.isAssignableFrom(loadedClass)) {
			logger.warn("Class " + loadedClass.getName() + " does not implement AiTask.");
			return false;
		}

		if (!loadedClass.isAnnotationPresent(PublicWorkflow.class)) {
			return false;
		}

		PublicWorkflow annotation = loadedClass.getAnnotation(PublicWorkflow.class);

		Class<?> configClass = classLoader.loadClass(annotation.configClass());

		// Class must implement the AiTaskConfig interface
		if (!AiTaskConfig.class.isAssignableFrom(configClass)) {
			logger.warn("Class " + loadedClass.getName() + " annotated configClass does not implement " + AiTaskConfig.class.getName());
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
			logger.warn("getTaskClassesFrom: caught exception: " + e);
		}
		return classes;
	}

	@Override
	public AiTask newTask(int userId, WorkflowRequest request) {
		if (!publicWorkflows.containsKey(request.getRequest())) {
			logger.warn("Workflow " + request.getRequest() + " does not exist.");
			return null;
		}
		logger.info("Running workflow " + request.getRequest());

		AiTask task = null;

		try {
			Class<?> taskClass = publicWorkflows.get(request.getRequest()).left;

			PublicWorkflow annotation = taskClass.getAnnotation(PublicWorkflow.class);
			Class<?> configClass = classLoader.loadClass(annotation.configClass());

			// First instantiate the config class with data.

			AiTaskConfig taskConfig = null;

			// If this request contains no config data, then just call the default constructor
			if (request.getData().isEmpty()) {
				Constructor<?> constructor = configClass.getDeclaredConstructor();
				taskConfig = (AiTaskConfig) constructor.newInstance();
			}
			else {
				Constructor<?>[] constructors = configClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isAssignableFrom(Map.class)) {
						taskConfig = (AiTaskConfig) constructor.newInstance(request.getData());
						break;
					}
				}
			}

			// Now instantiate the task, passing it the config object.

			// If the configClass is the NullTaskConfig class, then just call the default constructor
			if (configClass.equals(NullTaskConfig.class)) {
				Constructor<?> constructor = taskClass.getDeclaredConstructor();
				task = (AiTask) constructor.newInstance();
			}
			else {
				Constructor<?>[] constructors = taskClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isAssignableFrom(configClass)) {

						task = (AiTask) constructor.newInstance(configClass.cast(taskConfig));
					}
				}
			}

			if (task == null) {
				logger.warn("Failed to construct an instance of AiTask for request " + request.getRequest());
			}
			else {
				if (task instanceof ServiceConsumer) {
					((ServiceConsumer) task).setTaskServices(taskServices);
					((ServiceConsumer) task).setUserId(userId);
				}
			}

		} catch (SecurityException |
			InstantiationException |
			IllegalAccessException |
			IllegalArgumentException |
			InvocationTargetException e) {
			logger.warn("Failed to instantiate task for " + request.getRequest() + ": " + e);
			task = null;
		} catch (ClassNotFoundException e) {
			logger.warn("Failed to instantiate task for " + request.getRequest() + ". Class not found: " + e);
		} catch (NoSuchMethodException e) {
			logger.warn("Failed to instantiate class: " + e);
		}

		return task;

	}

	@Override
	public Map<String, Map<String, String>> getWorkflows() {
		Set<Entry<String, ImmutablePair<Class<?>, Map<String, AiTaskConfigTypes>>>> entrySet = publicWorkflows.entrySet();

		Map<String, Map<String, String>> result = new HashMap<>();

		entrySet.stream().forEach(entry -> {
			Map<String, String> workflowCfg = aiConfigMapToStringMap(entry.getValue().right);
			result.put(entry.getKey(), workflowCfg);
		});

		return result;
	}

	@Override
	public Map<String, String> getConfigFor(String workflowName) {
		if (!publicWorkflows.containsKey(workflowName)) {
			logger.warn("Workflow " + workflowName + " does not exist.");
			return null;
		}

		Class<?> taskClass = publicWorkflows.get(workflowName).left;
		PublicWorkflow annotation = taskClass.getDeclaredAnnotation(PublicWorkflow.class);

		try {
			Class<?> configClass = classLoader.loadClass(annotation.configClass());
			AiTaskConfig taskCfg = (AiTaskConfig) configClass.getDeclaredConstructor().newInstance();
			return aiConfigMapToStringMap(taskCfg.getConfig());

		} catch (ClassNotFoundException |
				InstantiationException |
				IllegalAccessException |
				IllegalArgumentException |
				InvocationTargetException |
				NoSuchMethodException |
				SecurityException e) {
			logger.warn("getConfigFor: Could not instantiate instance of " + annotation.configClass() + ": " + e);
			return null;
		}
	}

	private Map<String, String> aiConfigMapToStringMap(Map<String, AiTaskConfigTypes> aiConfigMap) {

		Map<String, String> workflowCfg = new HashMap<>();

		if (aiConfigMap != null) {
			aiConfigMap.entrySet().stream().forEach(innerEntry -> {
				workflowCfg.put(innerEntry.getKey(), innerEntry.getValue().toString());
			});
		}

		return workflowCfg;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

}
