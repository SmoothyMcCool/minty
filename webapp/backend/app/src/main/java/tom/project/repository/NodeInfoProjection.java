package tom.project.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import tom.api.model.project.NodeType;

public interface NodeInfoProjection {

	UUID getId();

	UUID getProjectId();

	NodeType getType();

	String getName();

	UUID getParentId();

	OffsetDateTime getCreated();

	OffsetDateTime getUpdated();
}
