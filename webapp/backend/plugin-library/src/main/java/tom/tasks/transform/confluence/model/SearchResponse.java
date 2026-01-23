package tom.tasks.transform.confluence.model;

import java.util.ArrayList;
import java.util.List;

public class SearchResponse {

	private List<SearchResult> results;

	// No-arg constructor
	public SearchResponse() {
		this.results = new ArrayList<>();
	}

	// All-args constructor
	public SearchResponse(List<SearchResult> results) {
		this.results = (results != null) ? results : new ArrayList<>();
	}

	// Getter and setter
	public List<SearchResult> getResults() {
		return results;
	}

	public void setResults(List<SearchResult> results) {
		this.results = (results != null) ? results : new ArrayList<>();
	}
}
