package tom.api.services;

import java.io.IOException;
import java.util.List;

import tom.api.ProjectEntryId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.Project;
import tom.api.model.project.ProjectEntry;
import tom.api.model.project.ProjectEntryInfo;

public interface ProjectService {

	Project createProject(UserId userId, String name);

	Project getProject(UserId userId, ProjectId projectId);

	List<ProjectEntryInfo> listProjectEntries(UserId userId, ProjectId projectId) throws IOException;

	ProjectEntry getProjectEntry(UserId userId, ProjectId projectId, ProjectEntryId entryId) throws IOException;

	void createOrUpdateProjectEntry(UserId userId, ProjectId projectId, ProjectEntry entry) throws IOException;

	List<Project> listProjects(UserId id);

	ProjectEntry getProjectEntryByName(UserId userId, ProjectId projectId, String fileName);

	void deleteProjectEntry(UserId userId, ProjectId projectId, ProjectEntryId entryId);

	void deleteProject(UserId userId, ProjectId projectId);

}