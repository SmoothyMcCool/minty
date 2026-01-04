package tom.project.repository;

import java.util.UUID;

import tom.api.model.project.ProjectEntryType;

public interface ProjectEntryInfoProjection {

	UUID getId();

	UUID getProjectId();

	ProjectEntryType getType();

	String getName();

	UUID getParentId();

}
