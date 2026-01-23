package tom.tasks.transform.confluence.model;

import java.util.ArrayList;
import java.util.List;

public class SearchRequest {

	private String query;
	private List<String> spaces;
	private int limit;

	// No-arg constructor
	public SearchRequest() {
		this.spaces = new ArrayList<>();
		this.limit = 5;
	}

	// All-args constructor
	public SearchRequest(String query, List<String> spaces, int limit) {
		this.query = query;
		this.spaces = (spaces != null) ? spaces : new ArrayList<>();
		this.limit = (limit > 0) ? limit : 5;
	}

	// Getters and setters
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public List<String> getSpaces() {
		return spaces;
	}

	public void setSpaces(List<String> spaces) {
		this.spaces = (spaces != null) ? spaces : new ArrayList<>();
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = (limit > 0) ? limit : 5;
	}
}
