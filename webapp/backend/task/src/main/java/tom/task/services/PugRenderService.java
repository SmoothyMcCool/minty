package tom.task.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface PugRenderService {

	Path render(String template, String outfileName, Map<String, Object> data) throws IOException;

}
