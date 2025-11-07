package tom.task;

import java.util.ArrayList;
import java.util.List;

public class TaskResult {

	private List<List<Packet>> results;

	public TaskResult(int numOutputs) {
		results = new ArrayList<>();
		for (int i = 0; i < numOutputs; i++) {
			results.add(new ArrayList<>());
		}
	}

	void addResult(int output, Packet result) {
		results.get(output).add(result);
	}

	void setResult(int output, List<Packet> result) {
		results.get(output).clear();
		results.get(output).addAll(result);
	}

	List<Packet> getResultForOutput(int output) {
		return results.get(output);
	}
}
