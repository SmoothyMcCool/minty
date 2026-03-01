package tom.api.model.project;

public class FileInfo {
	private FileType type;
	private String path;
	private int version;

	public FileInfo() {
		type = FileType.code;
		path = "";
		version = 0;
	}

	public FileInfo(FileType type, String path, int version) {
		this.type = type;
		this.path = path;
		this.version = version;
	}

	public FileType getType() {
		return type;
	}

	public void setType(FileType type) {
		this.type = type;
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
