package tom.plugins.loader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import tom.api.MintyProperties;
import tom.api.task.MintyTask;
import tom.api.task.OutputTask;
import tom.api.task.TaskConfigSpec;
import tom.api.task.annotation.Output;
import tom.api.task.annotation.RunnableTask;
import tom.api.task.enumspec.EnumSpecCreator;
import tom.api.tool.MintyTool;
import tom.config.MintyPropertiesImpl;
import tom.task.registry.TaskLoadFailureException;
import tom.task.registry.TaskRegistryService;
import tom.tool.registry.ToolLoadFailureException;
import tom.tool.registry.ToolRegistryService;

@Service
public class PluginLoader {

	private final Logger logger = LogManager.getLogger(PluginLoader.class);

	private final String pluginLibrary;

	private final ToolRegistryService toolRegistryService;
	private final TaskRegistryService taskRegistryService;
	private final MintyPropertiesImpl properties;

	public PluginLoader(ToolRegistryService toolRegistryService, TaskRegistryService taskRegistryService,
			MintyProperties properties) {
		this.toolRegistryService = toolRegistryService;
		this.taskRegistryService = taskRegistryService;
		this.properties = (MintyPropertiesImpl) properties;
		pluginLibrary = properties.get("pluginLibrary");
	}

	@PostConstruct
	public void initialize() {
		logger.info("Searching for plugins at " + pluginLibrary);

		try (Stream<Path> stream = Files.list(Path.of(pluginLibrary))) {
			List<Path> paths = stream.filter(file -> file.toString().endsWith(".jar")).toList();

			List<String> classes = new ArrayList<>();
			List<String> htmlFiles = new ArrayList<>();
			for (Path path : paths) {
				logger.info("Found library " + path.toString());
				classes.addAll(getFilesFrom(path, ".class"));
				htmlFiles.addAll(getFilesFrom(path, ".html"));
			}
			findAndRegisterAllTaskClasses(classes, paths);
			findAndRegisterAllEnumListClasses(classes, paths);
			findAndRegisterAllToolClasses(classes, paths);
			findAndRegisterAllHtmlFiles(htmlFiles, paths);

		} catch (Exception e) {
			logger.warn("initialize: failed to load plugin libraries. ", e);
			throw new RuntimeException("Failed to load plugins.");
		}
	}

	private void findAndRegisterAllTaskClasses(List<String> classNames, List<Path> jarPaths)
			throws ClassNotFoundException, TaskLoadFailureException, IOException {
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
					taskRegistryService.loadRunnableTask(loadedClass);

				} else if (isValidOutputTask(loadedClass)) {
					taskRegistryService.loadOutputTask(loadedClass);
				}
			}

			// Now that we have processed all classes, let's try to read out our default
			// values
			properties.readSystemDefaults();

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

					if (isValidEnumSpecCreator(loadedClass)) {
						taskRegistryService.loadEnumSpecCreator(loadedClass);
					}
				} catch (Exception e) {
					logger.warn("Failed to load EnumSpecCreator class: " + className, e);
				}
			}

		} catch (IOException | IllegalArgumentException | SecurityException e) {
			logger.warn("findAndRegisterAllTaskClasses: Failed to load class: ", e);
		}

	}

	private void findAndRegisterAllToolClasses(List<String> classNames, List<Path> jarPaths)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ToolLoadFailureException {
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
					toolRegistryService.loadTool(loadedClass);
				}
			}
		}

	}

	private void findAndRegisterAllHtmlFiles(List<String> fileNames, List<Path> jarPaths) {
		for (Path path : jarPaths) {
			try (JarFile jar = new JarFile(path.toFile())) {

				jar.stream().filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".html"))
						.forEach(entry -> {
							try (InputStream in = jar.getInputStream(entry)) {

								String fileName = entry.getName().replaceAll("^.*[\\\\/]", "").replaceAll("\\.[^.]+$",
										"");

								if (taskRegistryService.hasTask(fileName)) {
									String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
									taskRegistryService.addTaskHelp(fileName, content);
								} else if (taskRegistryService.hasOutputTask(fileName)) {
									String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
									taskRegistryService.addOutputTaskHelp(fileName, content);
								}
							} catch (Exception ex) {
								throw new RuntimeException("Failed to read " + entry.getName(), ex);
							}
						});
			} catch (IOException e) {
				logger.warn("findAndRegisterAllHtmlFiles: Could not load URL. ", e);
			}
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

	private boolean isValidTool(Class<?> loadedClass) throws ClassNotFoundException {
		if (!MintyTool.class.isAssignableFrom(loadedClass)) {
			return false;
		}

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		// Task must have at least one method tagged with the @Tool annotation.
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

	private boolean isValidEnumSpecCreator(Class<?> loadedClass) throws ClassNotFoundException {
		if (!EnumSpecCreator.class.isAssignableFrom(loadedClass)) {
			return false;
		}

		// Task must implement default constructor.
		if (!implementsDefaultConstructor(loadedClass.getConstructors())) {
			logger.warn("Class " + loadedClass.getName() + " does not implement default constructor.");
			return false;
		}

		return true;
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
