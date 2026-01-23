package tom.tasks.transform.confluence.model;

import org.springframework.ai.tool.annotation.ToolParam;

public class PageRequest {

	@ToolParam(description = "The ID of the page to fetch")
	private String pageId;

	// No-arg constructor
	public PageRequest() {
	}

	// All-args constructor
	public PageRequest(String pageId) {
		this.pageId = pageId;
	}

	// Getter and setter
	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}
}
