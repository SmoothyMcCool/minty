package tom.confluence.model;

import org.springframework.ai.tool.annotation.ToolParam;

public class ChildPage {

	@ToolParam(description = "The ID of the child page.")
	private String id;

	@ToolParam(description = "The title of the child page.")
	private String title;

	// No-arg constructor for frameworks
	public ChildPage() {
	}

	// All-args constructor
	public ChildPage(String id, String title) {
		this.id = id;
		this.title = title;
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
