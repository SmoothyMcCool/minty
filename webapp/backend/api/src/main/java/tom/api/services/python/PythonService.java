package tom.api.services.python;

import java.util.Map;

public interface PythonService {

	PythonResult execute(String pythonFile, Map<String, Object> inputDictionary) throws PythonException;

	PythonResult executeCodeString(String code, Map<String, Object> inputDictionary) throws PythonException;

}
