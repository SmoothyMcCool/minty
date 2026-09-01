package tom.api.services.assistant;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamResult implements LlmResult {

	public static record Chunk(ChunkType type, String name, String chunk) {
	}

	private final BlockingQueue<Chunk> chunks = new LinkedBlockingQueue<>();
	private AtomicReference<LlmMetric> metric = new AtomicReference<>();
	private AtomicReference<List<String>> sources = new AtomicReference<>();
	private final AtomicBoolean complete = new AtomicBoolean(false);
	private AtomicReference<LlmResultState> resultState = new AtomicReference<>();
	private final String query;
	private String name; // This is only used by agents to identify the name of the running sub-agent. It
							// is ignored for normal assistants.

	public StreamResult(String query) {
		resultState.set(LlmResultState.QUEUED);
		this.query = query;
		name = "";
	}

	@Override
	public String getQuery() {
		return query;
	}

	public void addChunk(String chunk, ChunkType type) {
		chunks.offer(new Chunk(type, name, chunk));
	}

	public Chunk takeChunk() throws InterruptedException {
		while (true) {
			Chunk chunk = chunks.poll(100, TimeUnit.MILLISECONDS);

			if (chunk != null) {
				return chunk;
			}

			if (isComplete()) {
				return null;
			}
		}
	}

	public boolean markComplete() {
		resultState.set(LlmResultState.COMPLETE);
		return complete.compareAndSet(false, true);
	}

	public boolean isComplete() {
		return complete.get() && chunks.isEmpty();
	}

	public void addUsage(LlmMetric llmMetric) {
		metric.set(llmMetric);
	}

	public LlmMetric getUsage() {
		return metric.get();
	}

	public List<String> getSources() {
		return sources.get();
	}

	public void addSources(List<String> docsUsed) {
		sources.set(List.copyOf(docsUsed));
	}

	public void setState(LlmResultState resultState) {
		this.resultState.set(resultState);
	}

	public LlmResultState getState() {
		return resultState.get();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
