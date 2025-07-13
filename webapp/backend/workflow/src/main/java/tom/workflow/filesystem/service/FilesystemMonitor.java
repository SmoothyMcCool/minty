package tom.workflow.filesystem.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.workflow.filesystem.repository.FilesystemWatcher;

public class FilesystemMonitor implements Runnable {

	private static final Logger logger = LogManager.getLogger(FilesystemMonitor.class);

	private final Iterable<FilesystemWatcher> watchers;
	private WatchService watcher;
	private final FilesystemWatcherService filesystemWatcherService;
	private boolean stop;
	private Map<WatchKey, Path> keyMap;

	public FilesystemMonitor(Iterable<FilesystemWatcher> watchers, FilesystemWatcherService filesystemWatcherService) {
		this.watchers = watchers;
		this.filesystemWatcherService = filesystemWatcherService;
		stop = false;
		keyMap = new HashMap<>();
	}

	@Override
	public void run() {

		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			logger.error("Failed to create Filesystem watcher!");
			return;
		}

		boolean nothingToWatch = true;
		for (FilesystemWatcher fsWatcher : watchers) {

			Path path = Paths.get(fsWatcher.getLocationToWatch());

			try {
				logger.info("Watching for new files in " + path);
				WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
				keyMap.put(key, path);
				nothingToWatch = false;
			} catch (IOException e) {
				logger.warn("Failed to register watcher for " + path + ": " + e);
			}
		}

		if (nothingToWatch) {
			logger.info("There are no filesystem watchers registered.");
			return;
		}

		while (!Thread.currentThread().isInterrupted() && !stop) {

			try {
				WatchKey key = watcher.poll(1, TimeUnit.SECONDS);

				if (key != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();

						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}

						@SuppressWarnings("unchecked")
						WatchEvent<Path> we = (WatchEvent<Path>) event;
						Path filename = we.context();
						Path directory = keyMap.get(key);

						// directory now holds the directory of the change we are monitoring
						for (FilesystemWatcher watcher : watchers) {
							if (watcher.getLocationToWatch().compareToIgnoreCase(directory.toString()) == 0) {
								logger.info("Found new file. Starting task: " + filename);
								filesystemWatcherService.startTaskFor(watcher, filename);
							}
						}

						boolean valid = key.reset();
						if (!valid) {
							logger.warn("Directory no longer watchable: " + directory);
							break;
						}
					}
				}

			} catch (InterruptedException e) {
				logger.info("FilesystemWatcherService thread was interrupted and will now stop.");
			}
		}

		try {
			watcher.close();
		} catch (IOException e) {
			logger.warn("Failed to close Filesystem watcher: " + e);
		}

	}

	public void stop() {
		stop = true;
	}
}
