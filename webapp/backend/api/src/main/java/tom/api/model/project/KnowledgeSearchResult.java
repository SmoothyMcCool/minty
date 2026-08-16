package tom.api.model.project;

import java.util.List;

public class KnowledgeSearchResult {

	private KnowledgeItemType type;
	private String name;
	private String path;
	private List<KnowledgeSearchMatch> matches;

	public KnowledgeSearchResult() {
	}

	public KnowledgeSearchResult(KnowledgeItemType type, String name, String path, List<KnowledgeSearchMatch> matches) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.matches = matches;
	}

	public KnowledgeItemType getType() {
		return type;
	}

	public void setType(KnowledgeItemType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<KnowledgeSearchMatch> getMatches() {
		return matches;
	}

	public void setMatches(List<KnowledgeSearchMatch> matches) {
		this.matches = matches;
	}
}