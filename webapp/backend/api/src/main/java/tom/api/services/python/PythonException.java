package tom.api.services.python;

import java.util.List;

public class PythonException extends Exception {

	private static final long serialVersionUID = 6254208933279250261L;
	private final String reason;
	private final List<String> logs;

	public PythonException(String reason, List<String> logs, Throwable cause) {
		super(cause);
		this.reason = reason;
		this.logs = logs;
	}

	public String getReason() {
		return reason;
	}

	public List<String> getLogs() {
		return logs;
	}

}
