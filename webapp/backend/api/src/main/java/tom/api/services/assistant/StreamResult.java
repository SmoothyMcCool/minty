package tom.api.services.assistant;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StreamResult implements LlmResult {

	private final BlockingQueue<String> chunks = new LinkedBlockingQueue<>();
	private volatile boolean complete = false;

	public StreamResult() {
	}

	public void addChunk(String chunk) {
		chunks.offer(chunk);
	}

	public String takeChunk() throws InterruptedException {
		while (true) {
			String chunk = chunks.poll(100, TimeUnit.MILLISECONDS);

			if (chunk != null) {
				return chunk;
			}

			if (isComplete()) {
				return null;
			}
		}
	}

	public void markComplete() {
		complete = true;
	}

	public boolean isComplete() {
		return complete && chunks.isEmpty();
	}
}
