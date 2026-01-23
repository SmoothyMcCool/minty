package tom.tools.project;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import tom.api.NodeId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.Node;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;

public class ProjectTools implements MintyTool, ServiceConsumer {

	private static final Logger logger = LogManager.getLogger(ProjectTools.class);

	private PluginServices pluginServices;
	private UserId userId;
	private ProjectId projectId;

	@Override
	public void initialize() {
		projectId = null;
		Map<String, String> userDefaults = pluginServices.getUserService().getUserDefaults(userId);

		String projectIdStr = userDefaults.getOrDefault("defaultProject", "");
		if (!projectIdStr.isBlank()) {
			projectId = new ProjectId(projectIdStr);
		}
	}

	@Tool(name = "validate_project_exists", description = "Checks if the project exists")
	MintyToolResponse<Boolean> projectExists() {
		boolean exists = false;
		try {
			if (projectId == null) {
				return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
			}

			pluginServices.getProjectService().getProject(userId, projectId);
			logger.info("Telling the LLM the project exists.");
			exists = true;
		} catch (Exception e) {
			logger.info("Telling the LLM the project does not exist.");
		}
		return MintyToolResponse.SuccessResponse(exists);
	}

	@Tool(name = "list_files_at_root", description = "List all files in the project")
	MintyToolResponse<List<NodeInfo>> listFiles() {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {
			List<NodeInfo> entries = pluginServices.getProjectService().listNodes(userId, projectId);
			logger.info("list_files returning: " + entries.stream().map(entry -> entry.getName()).toList().toString());
			return MintyToolResponse.SuccessResponse(entries);
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId + ". " + e.getMessage();
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}
	}

	@Tool(name = "list_files_in_folder", description = "List all files in the project")
	MintyToolResponse<List<NodeInfo>> listFilesInFolder(@ToolParam() String folderNodeId) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {
			List<NodeInfo> entries = pluginServices.getProjectService().listNodesUnderNode(userId, projectId,
					new NodeId(folderNodeId));
			logger.info("list_files_in_folder returning: "
					+ entries.stream().map(entry -> entry.getName()).toList().toString());
			return MintyToolResponse.SuccessResponse(entries);
		} catch (Exception e) {
			String error = "Failed to list files under folder " + folderNodeId + ", for project " + projectId + ". "
					+ e.getMessage();
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}
	}

	@Tool(name = "get_file_contents", description = "Get the contents of a specific file")
	MintyToolResponse<String> getFileContents(@ToolParam() String nodeId) {
		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		String response;
		Node node = pluginServices.getProjectService().getNode(userId, projectId, new NodeId(nodeId));

		if (node == null) {
			response = "Could not retrieve file.";
			return MintyToolResponse.FailureResponse(response);
		} else {
			response = node.toString();
		}

		logger.info("get_file_contents returning: " + response);
		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "create_file", description = "Create a new file")
	MintyToolResponse<NodeInfo> createFile(@ToolParam(description = "The name of the file.") String name,
			@ToolParam(required = false, description = "The parent of the new node. If null, it will be created at the root level. "
					+ "This parameter must be as supplied by the tool. You must create the parent first if you do not have a UUID already.") String parentId,
			@ToolParam() String fileContents) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {
			NodeInfo nodeInfo = new NodeInfo();
			nodeInfo.setName(name);
			nodeInfo.setParentId(new NodeId(parentId));
			nodeInfo.setType(NodeType.File);

			Node node = new Node(nodeInfo, fileContents);

			// This will throw if the parent doesn't exist.
			pluginServices.getProjectService().getNode(userId, projectId, new NodeId(parentId));

			pluginServices.getProjectService().createOrUpdateNode(userId, projectId, node);
			logger.info("createFile created " + name);

			return MintyToolResponse.SuccessResponse(nodeInfo);

		} catch (Exception e) {
			logger.warn("Failed to create file {} in project {}.", name, projectId);
			return MintyToolResponse
					.FailureResponse("Failed to create file. Ensure all folders in path exist. " + e.getMessage());
		}
	}

	@Tool(name = "update_file_information", description = "Update the metadata or location of an existing file")
	MintyToolResponse<NodeInfo> updateFileInfo(@ToolParam() String nodeId, @ToolParam(required = false) String fileName,
			@ToolParam(required = false, description = "The parent of the new node. If null, it will be created at the root level. "
					+ "This parameter must be as supplied by the tool. You must create the parent first if you do not have a UUID already from the project.") String parentId) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {

			Node node = pluginServices.getProjectService().getNode(userId, projectId, new NodeId(nodeId));

			if (node == null) {
				logger.warn("updateFileInfo: File does not exist project {}.", nodeId, projectId);
				return MintyToolResponse.FailureResponse("File does not exist at given nodeId");
			}

			NodeInfo nodeInfo = node.info();

			if (fileName != null) {
				nodeInfo.setName(fileName);
			}
			if (parentId != null) {
				nodeInfo.setParentId(new NodeId(parentId));
			}

			// This will throw if the parent doesn't exist.
			pluginServices.getProjectService().getNode(userId, projectId, new NodeId(parentId));

			pluginServices.getProjectService().updateNodeInfo(userId, projectId, nodeInfo);
			logger.info("updateFileInfo updated " + fileName);

			return MintyToolResponse.SuccessResponse(nodeInfo);

		} catch (Exception e) {
			logger.warn("Failed to update file {} in project {}.", fileName, projectId);
			return MintyToolResponse
					.FailureResponse("Failed to update file. Maybe not all parent folders exist? " + e.getMessage());
		}
	}

	@Tool(name = "update_file_contents", description = "Update the contents of an existing file")
	MintyToolResponse<NodeInfo> updateFileContents(@ToolParam() String nodeId, @ToolParam() String fileContents) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {

			Node node = pluginServices.getProjectService().getNode(userId, projectId, new NodeId(nodeId));

			if (node == null) {
				logger.warn("updateFileContents: File does not exist project {}.", nodeId, projectId);
				return MintyToolResponse.FailureResponse("File does not exist at given nodeId");
			}

			NodeInfo nodeInfo = node.info();
			Node updated = new Node(nodeInfo, fileContents);
			pluginServices.getProjectService().createOrUpdateNode(userId, projectId, updated);
			logger.info("updateFileContents updated " + nodeId);

			return MintyToolResponse.SuccessResponse(nodeInfo);

		} catch (Exception e) {
			logger.warn("Failed to update file {} in project {}.", nodeId, projectId);
			return MintyToolResponse.FailureResponse("Failed to update file contents. " + e.getMessage());
		}
	}

	@Tool(name = "create_folder", description = "Create a new folder")
	MintyToolResponse<NodeInfo> createFolder(@ToolParam() String folder,
			@ToolParam(required = false, description = "The parent of the new node. If null, it will be created at the root level. "
					+ "This parameter must be as supplied by the tool. You must create the parent first if you do not have a UUID already.") String parentId) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {

			NodeId parentNodeId = new NodeId(parentId);

			NodeInfo nodeInfo = new NodeInfo();
			nodeInfo.setName(folder);
			nodeInfo.setParentId(parentNodeId);
			nodeInfo.setType(NodeType.Folder);

			Node updated = new Node(nodeInfo, null);

			// This will throw if the parent doesn't exist.
			pluginServices.getProjectService().getNode(userId, projectId, parentNodeId);

			pluginServices.getProjectService().createOrUpdateNode(userId, projectId, updated);
			logger.info("createFolder created " + folder);

			return MintyToolResponse.SuccessResponse(nodeInfo);

		} catch (Exception e) {
			logger.warn("Failed to create folder {} in project {}.", folder, projectId);
			return MintyToolResponse.FailureResponse("Failed to create folder: " + e.getMessage());
		}
	}

	@Tool(name = "update_folder", description = "Update an existing folder")
	MintyToolResponse<NodeInfo> updateFolder(@ToolParam() String nodeId, @ToolParam() String folderName,
			@ToolParam(required = false, description = "The parent of the node. If null, it will be created at the root level.") String parentId) {

		if (projectId == null) {
			return MintyToolResponse.FailureResponse("User must set a default ProjectId set.");
		}

		try {

			Node entry = pluginServices.getProjectService().getNode(userId, projectId, new NodeId(nodeId));

			if (entry == null) {
				logger.warn("Folder {} in project {} doesn't exist.", folderName, projectId);
				return MintyToolResponse.FailureResponse("Folder specified by nodeId does not exist.");
			}

			NodeInfo nodeInfo = new NodeInfo();
			nodeInfo.setName(folderName);
			nodeInfo.setParentId(new NodeId(parentId));
			nodeInfo.setType(NodeType.Folder);

			Node updated = new Node(nodeInfo, null);

			// This will throw if the parent doesn't exist.
			pluginServices.getProjectService().getNode(userId, projectId, new NodeId(parentId));

			pluginServices.getProjectService().createOrUpdateNode(userId, projectId, updated);
			logger.info("updateFolder updated " + folderName);

			return MintyToolResponse.SuccessResponse(nodeInfo);

		} catch (Exception e) {
			logger.warn("Failed to update folder {} in project {}.", folderName, projectId);
			return MintyToolResponse.FailureResponse("Failed to update folder.");
		}
	}

	@Override
	public String name() {
		return "Project Tools";
	}

	@Override
	public String description() {
		return "A suite of tools to allow the LLM to interact with a Minty Project - reading, writing, and updating files.";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}

}
