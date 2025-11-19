package tom.workflow.executor;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import tom.task.OutputPort;
import tom.task.Packet;

public class Connector implements OutputPort {

	private BlockingDeque<Packet> unconsumedMessages;
	private boolean complete = false;
	private static Packet WRITING_COMPLETE = new Packet();

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

		Packet item = unconsumedMessages.takeFirst();
		if (item.equals(WRITING_COMPLETE)) {
			complete = true;
			return null;
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
