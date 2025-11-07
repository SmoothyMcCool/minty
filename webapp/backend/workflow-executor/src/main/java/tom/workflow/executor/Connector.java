package tom.workflow.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import tom.task.OutputPort;
import tom.task.Packet;

public class Connector implements OutputPort {

	private BlockingQueue<Packet> unconsumedMessages;
	private boolean complete = false;
	private static Packet WRITING_COMPLETE = new Packet();

	public Connector() {
		unconsumedMessages = new LinkedBlockingQueue<>();
	}

	@Override
	public void write(Packet message) {
		unconsumedMessages.offer(message);
	}

	public synchronized void complete() {
		unconsumedMessages.offer(WRITING_COMPLETE);
	}

	public Packet read() throws InterruptedException {
		if (complete) {
			return null;
		}

		Packet item = unconsumedMessages.take();
		if (item.equals(WRITING_COMPLETE)) {
			complete = true;
			return null;
		}
		return item;
	}

	public boolean isComplete() {
		return complete;
	}
}
