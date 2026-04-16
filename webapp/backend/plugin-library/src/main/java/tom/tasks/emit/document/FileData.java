package tom.tasks.emit.document;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileData {

	private String file;
	private String name;

	public FileData() {
		file = "";
		name = "";
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonProperty("File")
	private void unpackFile(Map<String, Object> fileWrapper) {
		if (fileWrapper == null) {
			return;
		}

		Object inner = fileWrapper.get("file");
		if (!(inner instanceof Map)) {
			return;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> fileMap = (Map<String, Object>) inner;

		this.file = (String) fileMap.get("file");
		this.name = (String) fileMap.get("name");
	}

}
