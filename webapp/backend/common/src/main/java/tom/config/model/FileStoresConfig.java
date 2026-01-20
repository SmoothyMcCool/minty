package tom.config.model;

import java.nio.file.Path;

public record FileStoresConfig(Path docs, Path temp, Path plugins, Path python, Path workflowLogs, Path pug,
		Path projectRoot) {
}
