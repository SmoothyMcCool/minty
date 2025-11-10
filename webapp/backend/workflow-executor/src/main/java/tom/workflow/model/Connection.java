package tom.workflow.model;

import java.util.UUID;

public class Connection {

	private UUID readerId;
	private int readerPort;
	private UUID writerId;
	private int writerPort;

	public Connection() {
		readerId = UUID.randomUUID();
		readerPort = 0;
		writerId = UUID.randomUUID();
		writerPort = 0;
	}

	public Connection(UUID reader, int readerPort, UUID writer, int writerPort) {
		this.readerId = reader;
		this.readerPort = readerPort;
		this.writerId = writer;
		this.writerPort = writerPort;
	}

	public UUID getReaderId() {
		return readerId;
	}

	public void setReaderId(UUID readerId) {
		this.readerId = readerId;
	}

	public int getReaderPort() {
		return readerPort;
	}

	public void setReaderPort(int readerPort) {
		this.readerPort = readerPort;
	}

	public UUID getWriterId() {
		return writerId;
	}

	public void setWriterId(UUID writerId) {
		this.writerId = writerId;
	}

	public int getWriterPort() {
		return writerPort;
	}

	public void setWriterPort(int writerPort) {
		this.writerPort = writerPort;
	}

}
