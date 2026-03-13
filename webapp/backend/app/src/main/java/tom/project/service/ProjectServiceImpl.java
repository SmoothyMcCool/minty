package tom.project.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tom.NotFoundException;
import tom.NotOwnedException;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
import tom.api.model.project.Project;
import tom.api.services.ProjectService;
import tom.project.repository.ProjectEntity;
import tom.project.repository.ProjectFileContent;
import tom.project.repository.ProjectFileContentRepository;
import tom.project.repository.ProjectNodeEntity;
import tom.project.repository.ProjectNodeRepository;
import tom.project.repository.ProjectRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectNodeRepository nodeRepository;
	private final ProjectFileContentRepository fileContentRepository;

	public ProjectServiceImpl(ProjectRepository projectRepository, ProjectNodeRepository nodeRepository,
			ProjectFileContentRepository fileContentRepository) {
		this.projectRepository = projectRepository;
		this.nodeRepository = nodeRepository;
		this.fileContentRepository = fileContentRepository;
	}

	@Override
	@Transactional
	public Project createProject(UserId userId, String name) {
		ProjectEntity project = new ProjectEntity();
		project.setName(name);
		project.setOwnerId(userId);
		project.setCreated(Instant.now());
		project.setUpdated(Instant.now());
		project = projectRepository.save(project);
		createRoot(userId, project);
		return project.toModel();
	}

	private void createRoot(UserId userId, ProjectEntity project) {
		ProjectNodeEntity root = new ProjectNodeEntity();
		root.setProjectId(project.getId());
		root.setParentId(null);
		root.setOwnerId(userId);
		root.setName("/");
		root.setPath("/");
		root.setType(NodeType.Folder);
		root.setVersion(0);
		root.setCreated(Instant.now());
		root.setUpdated(Instant.now());
		nodeRepository.save(root);
	}

	@Override
	@Transactional
	public void deleteProject(UserId userId, ProjectId projectId) {
		validateProjectAccess(userId, projectId);

		projectRepository.deleteById(projectId.value());
	}

	@Override
	public Project getProject(UserId userId, ProjectId projectId) {
		Optional<ProjectEntity> maybeProject = projectRepository.findById(projectId.getValue());

		if (maybeProject.isPresent() && maybeProject.get().getOwnerId().equals(userId)) {
			return maybeProject.get().toModel();
		}
		throw new NotFoundException("Project does not exist");
	}

	@Override
	public List<Project> listProjects(UserId id) {
		return projectRepository.findByOwnerId(id).stream().map(project -> project.toModel()).toList();
	}

	private void validateProjectAccess(UserId userId, ProjectId projectId) {
		Optional<ProjectEntity> project = projectRepository.findById(projectId.getValue());

		if (project.isEmpty()) {
			throw new NotFoundException("Project " + projectId.toString() + " does not exist");
		}

		if (!project.get().getOwnerId().equals(userId)) {
			throw new NotOwnedException("Project " + projectId.toString() + " isn't owned by this user");
		}
	}

	@Override
	public NodeContent readNode(UserId userId, ProjectId projectId, String path) {
		ProjectNodeEntity node = getRequiredNode(userId, projectId, path);

		if (node.getType() == NodeType.Folder) {
			return new NodeContent(node.getPath(), node.getVersion(), null, null);
		}

		ProjectFileContent content = fileContentRepository.findTopByNodeIdOrderByVersionDesc(node.getId())
				.orElseThrow(() -> new IllegalStateException("File content missing."));

		return new NodeContent(node.getPath(), node.getVersion(), node.getFileType(), content.getContent());
	}

	@Override
	@Transactional
	public NodeInfo writeFile(UserId userId, ProjectId projectId, String path, FileType fileType, String content) {
		ProjectNodeEntity existing = nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), path, userId)
				.orElse(null);

		if (existing == null) {

			ProjectNodeEntity parent = getParentNode(userId, projectId, path);

			ProjectNodeEntity node = new ProjectNodeEntity();
			node.setProjectId(projectId.getValue());
			node.setParentId(parent.getId());
			node.setOwnerId(userId);
			node.setName(extractName(path));
			node.setPath(path);
			node.setType(NodeType.File);
			node.setFileType(fileType);
			node.setVersion(1);
			node.setCreated(Instant.now());
			node.setUpdated(Instant.now());

			node = nodeRepository.save(node);

			insertFileVersion(userId, node.getId(), 1, content);

			return new NodeInfo(NodeType.File, fileType, path, 1);
		}

		if (existing.getType() != NodeType.File) {
			throw new IllegalStateException("Path exists but is not a file.");
		}

		int newVersion = existing.getVersion() + 1;

		existing.setVersion(newVersion);
		existing.setUpdated(Instant.now());
		existing.setFileType(fileType);

		nodeRepository.save(existing);

		insertFileVersion(userId, existing.getId(), newVersion, content);

		return new NodeInfo(NodeType.File, fileType, path, newVersion);
	}

	@Override
	@Transactional
	public NodeInfo createFolder(UserId userId, ProjectId projectId, String path) {
		if (nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), path, userId).isPresent()) {
			throw new IllegalStateException("Path already exists.");
		}

		ProjectNodeEntity parent = getParentNode(userId, projectId, path);

		ProjectNodeEntity folder = new ProjectNodeEntity();
		folder.setProjectId(projectId.getValue());
		folder.setParentId(parent.getId());
		folder.setOwnerId(userId);
		folder.setName(extractName(path));
		folder.setPath(path);
		folder.setType(NodeType.Folder);
		folder.setVersion(0);
		folder.setCreated(Instant.now());
		folder.setUpdated(Instant.now());

		nodeRepository.save(folder);

		return new NodeInfo(NodeType.Folder, null, path, 0);
	}

	@Override
	@Transactional
	public int deleteNode(UserId userId, ProjectId projectId, String path) {
		// Make sure node exists. Will throw if not.
		getRequiredNode(userId, projectId, path);

		List<ProjectNodeEntity> affected = nodeRepository
				.findByProjectIdAndPathStartingWithAndOwnerId(projectId.getValue(), path, userId);

		for (ProjectNodeEntity n : affected) {
			if (n.getType() == NodeType.File) {
				fileContentRepository.findByNodeIdOrderByVersionDesc(n.getId()).forEach(fileContentRepository::delete);
			}
		}

		nodeRepository.deleteAll(affected);

		return affected.size();
	}

	@Override
	@Transactional
	public NodeInfo updateNodeMetadata(UserId userId, ProjectId projectId, String oldPath, String newPath,
			FileType fileType) {

		ProjectNodeEntity existing = getRequiredNode(userId, projectId, oldPath);

		// Prevent path collisions
		if (!oldPath.equals(newPath)) {
			nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), newPath, userId).ifPresent(n -> {
				throw new IllegalStateException("Target path already exists.");
			});
		}

		int newVersion = existing.getVersion() + 1;

		existing.setPath(newPath);
		existing.setName(extractName(newPath));
		existing.setUpdated(Instant.now());
		existing.setVersion(newVersion);

		if (fileType != null && existing.getType() == NodeType.File) {
			existing.setFileType(fileType);
		}

		nodeRepository.save(existing);

		return new NodeInfo(existing.getType(), fileType, newPath, newVersion);
	}

	@Override
	@Transactional
	public NodeInfo moveNode(UserId userId, ProjectId projectId, String sourcePath, String targetPath) {
		ProjectNodeEntity source = getRequiredNode(userId, projectId, sourcePath);

		if (nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), targetPath, userId).isPresent()) {
			throw new IllegalStateException("Target path already exists.");
		}

		List<ProjectNodeEntity> subtree = nodeRepository
				.findByProjectIdAndPathStartingWithAndOwnerId(projectId.getValue(), sourcePath, userId);

		for (ProjectNodeEntity node : subtree) {

			String oldPath = node.getPath();

			if (oldPath.equals(sourcePath)) {
				node.setPath(targetPath);
			} else {
				String suffix = oldPath.substring(sourcePath.length());
				node.setPath(targetPath + suffix);
			}

			node.setUpdated(Instant.now());
			node.setVersion(node.getVersion() + 1);
		}

		nodeRepository.saveAll(subtree);

		return new NodeInfo(source.getType(), source.getFileType(), targetPath, source.getVersion() + 1);
	}

	@Override
	public NodeInfo describePath(UserId userId, ProjectId projectId, String path) {
		ProjectNodeEntity pne = getRequiredNode(userId, projectId, path);
		return new NodeInfo(pne.getType(), pne.getFileType(), pne.getPath(), pne.getVersion());
	}

	@Override
	@Transactional(readOnly = true)
	public List<NodeInfo> describeTree(UserId userId, ProjectId projectId) {
		return nodeRepository.findByProjectIdAndOwnerIdOrderByPathAsc(projectId.getValue(), userId).stream()
				.map(n -> new NodeInfo(n.getType(), n.getFileType(), n.getPath(), n.getVersion()))
				.collect(Collectors.toList());
	}

	@Override
	public List<NodeInfo> listChildren(UserId userId, ProjectId projectId, String path) {
		ProjectNodeEntity folder = getRequiredNode(userId, projectId, path);

		if (folder.getType() != NodeType.Folder) {
			throw new IllegalStateException("Path is not a folder.");
		}

		return nodeRepository.findByProjectIdAndParentIdAndOwnerId(projectId.getValue(), folder.getId(), userId)
				.stream().map(n -> new NodeInfo(n.getType(), n.getFileType(), n.getPath(), n.getVersion()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void importZip(UserId userId, ProjectId projectId, InputStream zipStream) throws IOException {
		validateProjectAccess(userId, projectId);

		try (ZipInputStream zis = new ZipInputStream(zipStream, StandardCharsets.UTF_8)) {
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();

				if (entryName.contains("..")) {
					throw new IllegalStateException("Invalid zip entry: " + entryName);
				}

				String path = "/" + entryName.replace("\\", "/");

				if (entry.isDirectory()) {
					ensureFolderPath(userId, projectId, path);
					continue;
				}

				String folderPath = path.substring(0, path.lastIndexOf("/"));
				ensureFolderPath(userId, projectId, folderPath);
				String content = readZipEntry(zis);
				FileType type = detectFileType(path);
				writeFile(userId, projectId, path, type, content);
				zis.closeEntry();
			}
		}
	}

	private void ensureFolderPath(UserId userId, ProjectId projectId, String path) {
		if (path == null || path.isEmpty() || path.equals("/")) {
			return;
		}

		String[] parts = path.split("/");
		String current = "";

		for (String part : parts) {
			if (part.isEmpty())
				continue;

			current += "/" + part;

			if (nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), current, userId).isEmpty()) {
				createFolder(userId, projectId, current);
			}
		}
	}

	private String readZipEntry(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}

		return sb.toString();
	}

	private FileType detectFileType(String path) {
		String lower = path.toLowerCase();

		if (lower.endsWith(".md")) {
			return FileType.markdown;
		}
		if (lower.endsWith(".json")) {
			return FileType.json;
		}
		if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
			return FileType.yaml;
		}
		if (lower.endsWith(".html")) {
			return FileType.html;
		}

		return FileType.text;
	}

	private void insertFileVersion(UserId userId, UUID nodeId, int version, String content) {
		ProjectFileContent fc = new ProjectFileContent();
		fc.setNodeId(nodeId);
		fc.setOwnerId(userId);
		fc.setVersion(version);
		fc.setContent(content);
		fc.setCreated(Instant.now());
		fileContentRepository.save(fc);
	}

	private ProjectNodeEntity getRequiredNode(UserId userId, ProjectId projectId, String path) {
		return nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), path, userId)
				.orElseThrow(() -> new IllegalStateException("Path not found: " + path));
	}

	private ProjectNodeEntity getParentNode(UserId userId, ProjectId projectId, String path) {

		int idx = path.lastIndexOf("/");
		if (idx <= 0) {
			return getRequiredNode(userId, projectId, "/");
		}

		String parentPath = path.substring(0, idx);
		return getRequiredNode(userId, projectId, parentPath);
	}

	private String extractName(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}

}
