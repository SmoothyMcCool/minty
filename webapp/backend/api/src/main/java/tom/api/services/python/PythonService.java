package tom.api.services.python;

import java.util.List;
import java.util.Map;

public interface PythonService {

	PythonResult execute(String pythonFile, List<Map<String, Object>> inputDictionary) throws PythonException;

	PythonResult executeCodeString(String code, List<Map<String, Object>> inputDictionary) throws PythonException;

}
