package tom.api.model.project;

import java.util.List;

public class FileSearchResult {

	private String path;
	private List<FileSearchMatch> matches;

	public FileSearchResult() {
	}

	public FileSearchResult(String path, List<FileSearchMatch> matches) {
		this.path = path;
		this.matches = matches;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<FileSearchMatch> getMatches() {
		return matches;
	}

	public void setMatches(List<FileSearchMatch> matches) {
		this.matches = matches;
	}
}