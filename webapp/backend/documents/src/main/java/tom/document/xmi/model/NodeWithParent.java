package tom.document.xmi.model;

import com.fasterxml.jackson.databind.JsonNode;

public class NodeWithParent {
	public JsonNode node;
	public JsonNode parent;

	public NodeWithParent(JsonNode node, JsonNode parent) {
		this.node = node;
		this.parent = parent;
	}
}
