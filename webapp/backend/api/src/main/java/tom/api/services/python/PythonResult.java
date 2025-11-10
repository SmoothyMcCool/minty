package tom.api.services.python;

import java.util.List;
import java.util.Map;

public record PythonResult(Map<String, Object> result, List<String> logs) {

}
