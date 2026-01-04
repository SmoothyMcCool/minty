package tom.api.services.python;

import tom.api.task.Packet;

public interface PythonService {

	PythonResult execute(String pythonFile, Packet input) throws PythonException;

	PythonResult executeCodeString(String code, Packet input) throws PythonException;

}
