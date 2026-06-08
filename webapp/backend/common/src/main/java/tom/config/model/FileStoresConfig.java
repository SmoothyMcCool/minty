package tom.config.model;

import java.nio.file.Path;

public record FileStoresConfig(Path temp, Path plugins, Path python, Path workflowLogs, Path pug, Path scripts,
		Path agents, Path projectRoot) {
}
