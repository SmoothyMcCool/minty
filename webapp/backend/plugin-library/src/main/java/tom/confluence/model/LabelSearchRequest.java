package tom.confluence.model;

import org.springframework.ai.tool.annotation.ToolParam;

public class LabelSearchRequest {

	@ToolParam(description = "The label to search for")
	private String label;

	@ToolParam(description = "Maximum number of results to return")
	private int limit;

	// No-arg constructor
	public LabelSearchRequest() {
		this.limit = 5; // default limit
	}

	// All-args constructor
	public LabelSearchRequest(String label, int limit) {
		this.label = label;
		this.limit = (limit <= 0) ? 5 : limit;
	}

	// Getters and setters
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = (limit <= 0) ? 5 : limit;
	}
}
