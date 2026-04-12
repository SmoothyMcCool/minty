package tom.assistant.service.agent.worker;

import java.util.HashMap;
import java.util.Map;

public class WorkerRegistry {

	private final Map<String, WorkerHandler> handlers = new HashMap<>();

	public void register(String worker, WorkerHandler handler) {
		handlers.put(worker, handler);
	}

	public WorkerHandler get(String worker) {
		WorkerHandler handler = handlers.get(worker);
		if (handler == null) {
			throw new IllegalArgumentException("Unknown worker: " + worker);
		}
		return handler;
	}
}