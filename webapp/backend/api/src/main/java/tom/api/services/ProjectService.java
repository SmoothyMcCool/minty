package tom.api.services;

import java.util.List;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.Project;

public interface ProjectService {

	Project createProject(UserId userId, String name);

	void deleteProject(UserId userId, ProjectId projectId);

	Project getProject(UserId userId, ProjectId projectId);

	List<Project> listProjects(UserId id);

	NodeContent readNode(UserId userId, ProjectId projectId, String path);

	NodeInfo writeFile(UserId userId, ProjectId projectId, String path, FileType fileType, String content);

	NodeInfo createFolder(UserId userId, ProjectId projectId, String path);

	int deleteNode(UserId userId, ProjectId projectId, String path);

	NodeInfo updateNodeMetadata(UserId id, ProjectId projectId, String oldPath, String newPath, FileType ft);

	NodeInfo moveNode(UserId userId, ProjectId projectId, String sourcePath, String targetPath);

	NodeInfo describePath(UserId userId, ProjectId projectId, String path);

	List<NodeInfo> describeTree(UserId userId, ProjectId projectId);

	List<NodeInfo> listChildren(UserId userId, ProjectId projectId, String path);

}