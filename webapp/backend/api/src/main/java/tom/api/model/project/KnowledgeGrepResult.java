package tom.api.model.project;

import java.util.List;

public class KnowledgeGrepResult {

	private final List<KnowledgeSearchResult> results;
	private final boolean truncated;

	public KnowledgeGrepResult(List<KnowledgeSearchResult> results, boolean truncated) {
		this.results = results;
		this.truncated = truncated;
	}

	public List<KnowledgeSearchResult> getResults() {
		return results;
	}

	public boolean isTruncated() {
		return truncated;
	}
}