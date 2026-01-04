package tom.workflow.executor;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import tom.api.task.OutputPort;
import tom.api.task.Packet;

public class Connector implements OutputPort {

	public static final Packet WRITING_COMPLETE = new Packet();

	private BlockingDeque<Packet> unconsumedMessages;
	private boolean complete = false;

	public Connector() {
		unconsumedMessages = new LinkedBlockingDeque<>();
	}

	@Override
	public void write(Packet message) {
		unconsumedMessages.offerLast(message);
	}

	public synchronized void complete() {
		unconsumedMessages.offerLast(WRITING_COMPLETE);
	}

	public Packet read() throws InterruptedException {
		if (complete) {
			return null;
		}

		Packet item = unconsumedMessages.pollFirst(1, TimeUnit.SECONDS);
		if (item != null && item.equals(WRITING_COMPLETE)) {
			complete = true;
			return WRITING_COMPLETE;
		}
		return item;
	}

	public void replace(Packet item) throws InterruptedException {
		unconsumedMessages.putFirst(item);
	}

	public boolean isComplete() {
		return complete;
	}
}
