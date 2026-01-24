package tom.confluence.model;

import org.springframework.ai.tool.annotation.ToolParam;

public class Ancestor {

	@ToolParam(description = "The ID of the ancestor page.")
	private String id;

	@ToolParam(description = "The title of the ancestor page.")
	private String title;

	// No-arg constructor needed for frameworks
	public Ancestor() {
	}

	// All-args constructor for convenience
	public Ancestor(String id, String title) {
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
