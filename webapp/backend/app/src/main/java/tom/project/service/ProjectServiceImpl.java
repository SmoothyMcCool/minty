package tom.project.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.NotFoundException;
import tom.NotOwnedException;
import tom.api.NodeId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.Node;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
import tom.api.model.project.Project;
import tom.api.services.ProjectService;
import tom.config.MintyConfiguration;
import tom.project.repository.NodeInfoProjection;
import tom.project.repository.NodeRepository;
import tom.project.repository.ProjectRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

	private final ProjectRepository projectRepository;
	private final NodeRepository nodeRepository;

	public ProjectServiceImpl(ProjectRepository projectRepository, NodeRepository nodeRepository,
			MintyConfiguration properties) {
		this.projectRepository = projectRepository;
		this.nodeRepository = nodeRepository;
	}

	@Override
	@Transactional
	public Project createProject(UserId userId, String name) {
		tom.project.repository.Project project = new tom.project.repository.Project();
		project.setName(name);
		project.setOwnerId(userId);
		project = projectRepository.save(project);

		// Start the project with an empty root folder.
		NodeInfo nodeInfo = new NodeInfo();
		nodeInfo.setName("/");
		nodeInfo.setType(NodeType.Folder);
		nodeInfo.setParentId(null);
		Node entry = new Node(nodeInfo, null);
		createOrUpdateNode(userId, new ProjectId(project.getId()), entry);

		return project.toModel();
	}

	@Override
	@Transactional
	public void deleteProject(UserId userId, ProjectId projectId) {
		validateProjectAccess(userId, projectId);

		projectRepository.deleteById(projectId.value());
	}

	@Override
	public List<Project> listProjects(UserId id) {
		return projectRepository.findAllByOwnerId(id).stream().map(project -> project.toModel()).toList();
	}

	@Override
	public Project getProject(UserId userId, ProjectId projectId) {
		Optional<tom.project.repository.Project> maybeProject = projectRepository.findById(projectId.getValue());

		if (maybeProject.isPresent() && maybeProject.get().getOwnerId().equals(userId)) {
			return maybeProject.get().toModel();
		}
		throw new NotFoundException("Project does not exist");
	}

	@Override
	public List<NodeInfo> listNodes(UserId userId, ProjectId projectId) {
		validateProjectAccess(userId, projectId);

		List<NodeInfoProjection> entries = nodeRepository.findAllProjectedByProjectId(projectId.value());
		return entries.stream().map(entry -> new NodeInfo(new NodeId(entry.getId()), entry.getType(), entry.getName(),
				new NodeId(entry.getParentId()), entry.getCreated(), entry.getUpdated())).toList();
	}

	@Override
	public List<NodeInfo> listNodesUnderNode(UserId userId, ProjectId projectId, NodeId nodeId) {
		validateProjectAccess(userId, projectId);

		List<NodeInfoProjection> entries = nodeRepository.findAllProjectedByParentId(nodeId.value());
		return entries.stream().map(entry -> new NodeInfo(new NodeId(entry.getId()), entry.getType(), entry.getName(),
				new NodeId(entry.getParentId()), entry.getCreated(), entry.getUpdated())).toList();
	}

	@Override
	public Node getNode(UserId userId, ProjectId projectId, NodeId nodeId) {
		tom.project.repository.Node entry = getNodeInternal(userId, projectId, nodeId);
		return new Node(new NodeInfo(nodeId, entry.getType(), entry.getName(), new NodeId(entry.getParentId()),
				entry.getCreated(), entry.getUpdated()), entry.getData());
	}

	@Override
	@Transactional
	public void createOrUpdateNode(UserId userId, ProjectId projectId, Node node) {
		validateProjectAccess(userId, projectId);

		tom.project.repository.Node newNode;
		if (entryExists(node.info())) {
			// This will throw if we are not allowed to access the entry. We already know it
			// exists.
			newNode = getNodeInternal(userId, projectId, node.info().getNodeId());
		} else {
			newNode = new tom.project.repository.Node();
		}

		if (node.info().getParentId() != null) {
			// This will throw if the parent specified does not exist or isn't part of the
			// project.
			getNodeInternal(userId, projectId, node.info().getParentId());
		}

		newNode.setName(node.info().getName().trim());
		if (newNode.getName().isEmpty()) {
			throw new IllegalArgumentException("Name cannot be empty");
		}

		newNode.setProjectId(projectId.value());
		newNode.setType(node.info().getType());
		if (node.info().getParentId() != null) {
			newNode.setParentId(node.info().getParentId().value());
		}
		newNode.setData(node.data());
		nodeRepository.save(newNode);

	}

	@Override
	@Transactional
	public void deleteNode(UserId userId, ProjectId projectId, NodeId nodeId) {
		validateProjectAccess(userId, projectId);

		tom.project.repository.Node node = nodeRepository.findById(nodeId.value())
				.orElseThrow(() -> new NoSuchElementException("Node not found"));

		List<tom.project.repository.Node> children = nodeRepository.findAllByParentId(node.getId());
		for (tom.project.repository.Node child : children) {
			deleteNode(userId, projectId, new NodeId(child.getId()));
		}

		nodeRepository.deleteById(nodeId.value());
	}

	@Override
	@Transactional
	public void updateNodeInfo(UserId userId, ProjectId projectId, NodeInfo nodeInfo) {
		validateProjectAccess(userId, projectId);

		UUID parentId = nodeInfo.getParentId() != null ? nodeInfo.getParentId().value() : null;
		int updated = nodeRepository.updateNodeInfo(nodeInfo.getNodeId().value(), parentId, nodeInfo.getName());
		if (updated != 1) {
			throw new RuntimeException("Update failed");
		}
	}

	private void validateProjectAccess(UserId userId, ProjectId projectId) {
		Optional<tom.project.repository.Project> project = projectRepository.findById(projectId.getValue());

		if (project.isEmpty()) {
			throw new NotFoundException("Project " + projectId.toString() + " does not exist");
		}

		if (!project.get().getOwnerId().equals(userId)) {
			throw new NotOwnedException("Project " + projectId.toString() + " isn't owned by this user");
		}
	}

	private boolean entryExists(NodeInfo nodeInfo) {
		if (nodeInfo.getNodeId() == null) {
			return false;
		}
		return nodeRepository.existsById(nodeInfo.getNodeId().value());
	}

	private tom.project.repository.Node getNodeInternal(UserId userId, ProjectId projectId, NodeId nodeId) {
		validateProjectAccess(userId, projectId);

		Optional<tom.project.repository.Node> maybeEntry = nodeRepository.findById(nodeId.value());

		if (maybeEntry.isEmpty()) {
			throw new NotFoundException("File " + nodeId.value() + " does not exist");
		}

		tom.project.repository.Node entry = maybeEntry.get();

		if (!entry.getProjectId().equals(projectId.value())) {
			throw new NotOwnedException("Requested Node not part of this project.");
		}

		return entry;
	}

}
