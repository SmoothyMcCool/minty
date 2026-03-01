package tom.api.model.project;

public class NodeInfo {
	private NodeType type;
	private FileType fileType;
	private String path;
	private int version;

	public NodeInfo() {
		type = NodeType.File;
		fileType = FileType.text;
		path = "";
		version = 0;
	}

	public NodeInfo(NodeType type, FileType fileType, String path, int version) {
		this.type = type;
		this.fileType = fileType;
		this.path = path;
		this.version = version;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
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

}
