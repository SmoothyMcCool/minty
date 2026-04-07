package tom.api.services.workflow;

import java.util.UUID;

public class Connection {

	private String readerId;
	private int readerPort;
	private String writerId;
	private int writerPort;

	public Connection() {
		readerId = UUID.randomUUID().toString();
		readerPort = 0;
		writerId = UUID.randomUUID().toString();
		writerPort = 0;
	}

	public Connection(String reader, int readerPort, String writer, int writerPort) {
		this.readerId = reader;
		this.readerPort = readerPort;
		this.writerId = writer;
		this.writerPort = writerPort;
	}

	public String getReaderId() {
		return readerId;
	}

	public void setReaderId(String readerId) {
		this.readerId = readerId;
	}

	public int getReaderPort() {
		return readerPort;
	}

	public void setReaderPort(int readerPort) {
		this.readerPort = readerPort;
	}

	public String getWriterId() {
		return writerId;
	}

	public void setWriterId(String writerId) {
		this.writerId = writerId;
	}

	public int getWriterPort() {
		return writerPort;
	}

	public void setWriterPort(int writerPort) {
		this.writerPort = writerPort;
	}

}
