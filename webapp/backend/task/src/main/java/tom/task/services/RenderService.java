package tom.task.services;

import java.nio.file.Path;
import java.util.Map;

public interface RenderService {

    void renderPugTemplate(String pugTemplate, Map<String, Object> results, Path resultFile);

    void renderWordTemplate(String pugTemplate, Map<String, Object> results, Path resultFile);
}
