package tom.api.model.project;

public class NodeContent {

	private String path;
	private int version;
	private FileType fileType;

	private Integer startLine;
	private Integer endLine;
	private int totalLines;

	private String content;

	public NodeContent() {
	}

	// Complete file response.
	public NodeContent(String path, int version, FileType fileType, String content) {
		this(path, version, fileType, null, null, countLines(content), content);
	}

	// File fragment response.
	public NodeContent(String path, int version, FileType fileType, Integer startLine, Integer endLine, int totalLines,
			String content) {
		this.path = path;
		this.version = version;
		this.fileType = fileType;
		this.startLine = startLine;
		this.endLine = endLine;
		this.totalLines = totalLines;
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

	public Integer getStartLine() {
		return startLine;
	}

	public void setStartLine(Integer startLine) {
		this.startLine = startLine;
	}

	public Integer getEndLine() {
		return endLine;
	}

	public void setEndLine(Integer endLine) {
		this.endLine = endLine;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	private static int countLines(String content) {
		if (content == null || content.isEmpty()) {
			return 0;
		}

		int count = 1;

		for (int i = 0; i < content.length(); i++) {
			if (content.charAt(i) == '\n') {
				count++;
			}
		}

		// Don't count the empty line after a trailing newline.
		if (content.endsWith("\n")) {
			count--;
		}

		return count;
	}
}