package tom.task.services;

import java.util.Map;

public interface PythonService {

	Map<String, Object> execute(String pythonFile, Map<String, String> inputDictionary);

	Map<String, Object> executeCodeString(String code, Map<String, String> inputDictionary);

}
