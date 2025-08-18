package tom.api.services;

import java.util.Map;

public interface PythonService {

	Map<String, Object> execute(String pythonFile, Map<String, Object> inputDictionary);

	Map<String, Object> executeCodeString(String code, Map<String, Object> inputDictionary);

}
