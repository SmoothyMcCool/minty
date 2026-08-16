package tom.api.model.document;

import java.util.List;

public class DocumentSearchResult {

	private String title;
	private List<DocumentSearchMatch> matches;

	public DocumentSearchResult() {
	}

	public DocumentSearchResult(String title, List<DocumentSearchMatch> matches) {
		this.title = title;
		this.matches = matches;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<DocumentSearchMatch> getMatches() {
		return matches;
	}

	public void setMatches(List<DocumentSearchMatch> matches) {
		this.matches = matches;
	}
}