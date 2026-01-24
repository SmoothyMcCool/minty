package tom.confluence.model;

import java.util.List;

import org.springframework.ai.tool.annotation.ToolParam;

public class ChildrenResponse {

	@ToolParam(description = "The list of child pages.")
	private List<ChildPage> children;

	// No-arg constructor
	public ChildrenResponse() {
	}

	// All-args constructor
	public ChildrenResponse(List<ChildPage> children) {
		this.children = children;
	}

	// Getter and setter
	public List<ChildPage> getChildren() {
		return children;
	}

	public void setChildren(List<ChildPage> children) {
		this.children = children;
	}
}
