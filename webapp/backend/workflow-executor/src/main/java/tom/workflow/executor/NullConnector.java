package tom.workflow.executor;

import tom.api.task.Packet;

public class NullConnector extends Connector {

	public NullConnector() {
	}

	@Override
	public void write(Packet message) {
	}

	public synchronized void complete() {
	}

	public Packet read() throws InterruptedException {
		return null;
	}

	public boolean isComplete() {
		return true;
	}
}
