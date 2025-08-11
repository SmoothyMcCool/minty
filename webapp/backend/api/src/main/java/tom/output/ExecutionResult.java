package tom.output;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.model.ChatMessage;

public class ExecutionResult {

	private Instant startTime;
	private Instant endTime;
	private List<List<Map<String, Object>>> results;
	private List<List<ChatMessage>> chats = List.of();

	public ExecutionResult(int numSteps) {
		results = new ArrayList<>();
		for (int i = 0; i < numSteps; i++) {
			results.add(new ArrayList<>());
		}
	}

	public void addResult(int step, Map<String, Object> result) {
		results.get(step).add(result);
	}

	public Map<String, Object> getResults() {
		Map<String, Object> res = new HashMap<>();
		res.put("startTime", startTime);
		res.put("endTime", endTime);
		res.put("results", results);

		if (!chats.isEmpty()) {
			res.put("conversations", chats);
		}

		return res;
	}

	public void start() {
		startTime = Instant.now();
	}

	public void stop() {
		endTime = Instant.now();
	}

	public void setChats(List<List<ChatMessage>> chats) {
		this.chats = chats;
	}
}
