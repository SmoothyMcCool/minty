package tom.api.services;

import java.util.List;

import tom.api.NodeId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.Node;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.Project;

public interface ProjectService {

	Project createProject(UserId userId, String name);

	Project getProject(UserId userId, ProjectId projectId);

	List<Project> listProjects(UserId id);

	void deleteProject(UserId userId, ProjectId projectId);

	List<NodeInfo> listNodes(UserId userId, ProjectId projectId);

	Node getNode(UserId userId, ProjectId projectId, NodeId nodeId);

	void createOrUpdateNode(UserId userId, ProjectId projectId, Node node);

	void deleteNode(UserId userId, ProjectId projectId, NodeId nodeId);

	void updateNodeInfo(UserId userId, ProjectId projectId, NodeInfo nodeInfo);

}