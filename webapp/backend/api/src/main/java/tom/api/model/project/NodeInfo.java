package tom.api.model.project;

import java.time.OffsetDateTime;

import tom.api.NodeId;

public class NodeInfo {
	private NodeId nodeId;
	private NodeType type;
	private String name;
	private NodeId parentId;
	private OffsetDateTime created;
	private OffsetDateTime updated;

	public NodeInfo() {
		nodeId = null;
		type = NodeType.File;
		name = "";
		parentId = null;
		created = OffsetDateTime.now();
		updated = OffsetDateTime.now();
	}

	public NodeInfo(NodeId id, NodeType type, String name, NodeId parentId, OffsetDateTime created,
			OffsetDateTime updated) {
		this.nodeId = id;
		this.type = type;
		this.name = name;
		this.parentId = parentId;
		this.created = created;
		this.updated = updated;
	}

	public NodeId getNodeId() {
		return nodeId;
	}

	public void setNodeId(NodeId id) {
		this.nodeId = id;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NodeId getParentId() {
		return parentId;
	}

	public void setParentId(NodeId parentId) {
		this.parentId = parentId;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(OffsetDateTime updated) {
		this.updated = updated;
	}

}
