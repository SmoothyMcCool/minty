package tom.confluence.model;

import org.springframework.ai.tool.annotation.ToolParam;

public class ChildrenRequest {

	@ToolParam(description = "The ID of the parent page.")
	private String pageId;

	@ToolParam(description = "The maximum number of child pages to return. Defaults to 10 if not positive.")
	private int limit;

	// No-arg constructor
	public ChildrenRequest() {
		this.limit = 10; // default
	}

	// All-args constructor
	public ChildrenRequest(String pageId, int limit) {
		this.pageId = pageId;
		this.limit = (limit <= 0) ? 10 : limit;
	}

	// Getters and setters
	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = (limit <= 0) ? 10 : limit;
	}
}
