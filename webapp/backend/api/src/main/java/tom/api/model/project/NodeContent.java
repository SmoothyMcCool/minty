package tom.api.model.project;

public class NodeContent {

	private String path;
	private int version;
	private FileType fileType;
	private String content;

	public NodeContent() {
	}

	public NodeContent(String path, int version, FileType fileType, String content) {
		this.path = path;
		this.version = version;
		this.fileType = fileType;
		this.content = content;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
