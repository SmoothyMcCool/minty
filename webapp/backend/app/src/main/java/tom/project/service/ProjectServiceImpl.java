package tom.project.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
import tom.api.services.ProjectService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.project.model.Project;
import tom.project.model.ProjectFileContent;
import tom.project.model.ProjectNodeEntity;
import tom.project.repository.ProjectFileContentRepository;
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
	public tom.api.model.project.Project createProject(UserId userId, String name) {
		Project project = new Project();
		project.setName(name);
		project.setOwnerId(userId);
		project.setCreated(Instant.now());
		project.setUpdated(Instant.now());
		project = projectRepository.save(project);
		createRoot(userId, project);
		return project.toModel();
	}

	private void createRoot(UserId userId, Project project) {
		ProjectNodeEntity root = new ProjectNodeEntity();
		root.setProjectId(project.getId().value());
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
	public void deleteProject(UserId userId, ProjectId projectId) throws NotFoundException, NotOwnedException {
		validateProjectAccess(userId, projectId);

		projectRepository.deleteById(projectId.value());
	}

	@Override
	public tom.api.model.project.Project getProject(UserId userId, ProjectId projectId) throws NotFoundException {
		Optional<Project> maybeProject = projectRepository.findById(projectId.getValue());

		if (maybeProject.isPresent() && maybeProject.get().getOwnerId().equals(userId)) {
			return maybeProject.get().toModel();
		}
		throw new NotFoundException("Project does not exist");
	}

	@Override
	public List<tom.api.model.project.Project> listProjects(UserId id) {
		return projectRepository.findByOwnerId(id).stream().map(project -> project.toModel()).toList();
	}

	private void validateProjectAccess(UserId userId, ProjectId projectId) throws NotFoundException, NotOwnedException {
		Optional<Project> project = projectRepository.findById(projectId.getValue());

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
		ProjectNodeEntity node = getRequiredNode(userId, projectId, path);
		nodeRepository.delete(node);
		return 1;
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
	public List<NodeInfo> searchByFilter(UserId userId, ProjectId projectId, String filter) {
		if (filter == null || filter.isEmpty()) {
			// Handle empty filter case: return all nodes or an empty list
			return nodeRepository.findByProjectIdAndOwnerId(projectId.getValue(), userId).stream()
					.map(n -> new NodeInfo(n.getType(), n.getFileType(), n.getPath(), n.getVersion()))
					.collect(Collectors.toList());
		}

		// '*' becomes '%' (matches any sequence)
		// '?' becomes '_' (matches any single character)
		String sqlPattern = "%" + filter.replace("*", "%").replace("?", "_") + "%";

		List<ProjectNodeEntity> matchingNodes = nodeRepository.searchByFilter(projectId.getValue(), userId, sqlPattern);

		return matchingNodes.stream().map(n -> new NodeInfo(n.getType(), n.getFileType(), n.getPath(), n.getVersion()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void importZip(UserId userId, ProjectId projectId, InputStream zipStream)
			throws IOException, NotFoundException, NotOwnedException {
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

	private static final Map<String, FileType> EXTENSION_MAP = Map.ofEntries(
			// MARKDOWN
			Map.entry("md", FileType.markdown), Map.entry("markdown", FileType.markdown),

			// CODE (source‑code / scripts)
			Map.entry("ts", FileType.code), Map.entry("tsx", FileType.code), Map.entry("js", FileType.code),
			Map.entry("jsx", FileType.code), Map.entry("cjs", FileType.code), Map.entry("mjs", FileType.code),
			Map.entry("css", FileType.code), Map.entry("scss", FileType.code), Map.entry("sass", FileType.code),

			Map.entry("py", FileType.code), Map.entry("rb", FileType.code), Map.entry("php", FileType.code),
			Map.entry("go", FileType.code), Map.entry("rs", FileType.code), Map.entry("java", FileType.code),
			Map.entry("kt", FileType.code), Map.entry("kts", FileType.code), Map.entry("cs", FileType.code),
			Map.entry("cpp", FileType.code), Map.entry("cxx", FileType.code), Map.entry("cc", FileType.code),
			Map.entry("c", FileType.code), Map.entry("h", FileType.code), Map.entry("hpp", FileType.code),

			Map.entry("sh", FileType.code), Map.entry("bash", FileType.code), Map.entry("ps1", FileType.code),
			Map.entry("sql", FileType.code), Map.entry("bat", FileType.code), Map.entry("cmd", FileType.code),

			Map.entry("swift", FileType.code), Map.entry("scala", FileType.code), Map.entry("groovy", FileType.code),
			Map.entry("dart", FileType.code), Map.entry("lua", FileType.code), Map.entry("pl", FileType.code),
			Map.entry("pm", FileType.code), Map.entry("vb", FileType.code), Map.entry("f90", FileType.code),
			Map.entry("for", FileType.code), Map.entry("f95", FileType.code), Map.entry("hs", FileType.code),
			Map.entry("erl", FileType.code), Map.entry("ex", FileType.code), Map.entry("exs", FileType.code),
			Map.entry("r", FileType.code), Map.entry("R", FileType.code),

			// HTML
			Map.entry("html", FileType.html), Map.entry("htm", FileType.html),

			// JSON / YAML
			Map.entry("json", FileType.json), Map.entry("yaml", FileType.yaml), Map.entry("yml", FileType.yaml),

			// TEXT (config, data)
			Map.entry("xml", FileType.text), Map.entry("csv", FileType.text), Map.entry("tsv", FileType.text),
			Map.entry("toml", FileType.text), Map.entry("ini", FileType.text),

			// DIAGRAM / graph description
			Map.entry("mmd", FileType.diagram), Map.entry("mermaid", FileType.diagram),
			Map.entry("dot", FileType.diagram), Map.entry("plantuml", FileType.diagram),
			Map.entry("puml", FileType.diagram));

	private FileType detectFileType(String path) {
		String lower = path.toLowerCase(Locale.ROOT);

		// Special file names without an extension (Dockerfile)
		int slashIdx = lower.lastIndexOf('/');
		String nameOnly = (slashIdx >= 0 ? lower.substring(slashIdx + 1) : lower);
		if ("dockerfile".equals(nameOnly)) {
			return FileType.code;
		}

		// Extension lookup
		// Grab the last component after the final dot – if any
		int dotIdx = lower.lastIndexOf('.');
		if (dotIdx >= 0 && dotIdx < lower.length() - 1) {
			String ext = lower.substring(dotIdx + 1);
			FileType mapped = EXTENSION_MAP.get(ext);
			if (mapped != null) {
				return mapped;
			}
		}

		return FileType.text; // everything else is plain text
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
		// Make sure the path starts with "/" since some paths come from the LLM that
		// doesn't know the rules.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		final String finalStringForLambda = path;

		return nodeRepository.findByProjectIdAndPathAndOwnerId(projectId.getValue(), path, userId)
				.orElseThrow(() -> new IllegalStateException("Path not found: " + finalStringForLambda));
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
