package tom.task.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface RenderService {

	Path renderPug(String template, String outfileName, Map<String, Object> data) throws IOException;

	Path renderJson(String outfileName, Map<String, Object> data) throws IOException;
}
