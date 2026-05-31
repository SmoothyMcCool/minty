package tom.tools.project;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.conversation.Conversation;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;

@Component
public class ProjectTools implements MintyTool, ServiceConsumer {

	private PluginServices pluginServices;
	private UserId userId;
	private ConversationId conversationId;
	private ProjectId projectId;

	@Override
	public void initialize() {
		projectId = null;
		if (conversationId != null) {
			Conversation conversation = pluginServices.getConversationService().getConversation(userId, conversationId);
			projectId = conversation.getProjectId();
		}
	}

	private void ensureProjectSelected() {
		if (projectId == null) {
			throw new IllegalStateException("No project selected. User has no default project.");
		}
	}

	// ---------------------------------------------------------------------
	// LIST DIRECTORY
	// ---------------------------------------------------------------------

	@Tool(name = "project_list_folder", description = """
			List direct children of a folder.

			Arguments:
			- path: absolute folder path

			Returns:
			- immediate child files and folders

			Not recursive.

			Fails if:
			- path does not exist
			- path is not a folder

			Examples:
			- "/src"
			- "/docs"
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> listDirectory(
			@ToolParam(description = "Absolute folder path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().listChildren(userId, projectId, path));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// GET PROJECT TREE
	// ---------------------------------------------------------------------

	@Tool(name = "project_get_tree", description = """
			Return the complete project tree.

			Returns:
			- all project nodes
			- node types
			- versions

			Useful for inspecting overall project structure.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> getProjectTree() {
		try {
			ensureProjectSelected();
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().describeTree(userId, projectId));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// READ FILE
	// ---------------------------------------------------------------------

	@Tool(name = "project_read_file", description = """
			Read a file.

			Arguments:
			- path: absolute file path

			Returns:
			- full file contents
			- metadata
			- version

			Fails if:
			- path does not exist
			- path is a folder

			Examples:
			- "/src/Main.java"
			- "/README.md"
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<NodeContent> readFile(@ToolParam(description = "Absolute file path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			NodeContent result = pluginServices.getProjectService().readNode(userId, projectId, path);
			if (result.getFileType() == null) {
				return MintyToolResponse.FailureResponse("Path refers to a folder.");
			}
			return MintyToolResponse.SuccessResponse(result);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// WRITE FILE
	// ---------------------------------------------------------------------

	@Tool(name = "project_write_file", description = """
			Create or fully replace a file.

			Arguments:
			- path: absolute file path
			- fileType: code | markdown | json | text | diagram
			- content: complete final file contents

			Behavior:
			- creates missing files
			- fully replaces existing files
			- does not append
			- does not patch

			Parent folder must already exist.

			Fails if:
			- parent folder does not exist
			- path is a folder
			- fileType is invalid

			Examples:
			- "/src/App.java"
			- "/docs/design.md"
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> writeFile(@ToolParam(description = "Absolute file path") String path,
			@ToolParam(description = "One of: code, markdown, json, text, diagram") String fileType,
			@ToolParam(description = "Complete final file contents") String content) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			FileType parsedType;
			try {
				parsedType = FileType.valueOf(fileType);
			} catch (Exception e) {
				return MintyToolResponse
						.FailureResponse("Invalid fileType. Must be one of: code, markdown, json, text, diagram");
			}
			return MintyToolResponse.SuccessResponse(
					pluginServices.getProjectService().writeFile(userId, projectId, path, parsedType, content));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// CREATE FOLDER
	// ---------------------------------------------------------------------

	@Tool(name = "project_create_folder", description = """
			Create a folder.

			Arguments:
			- path: absolute folder path

			Parent folder must already exist.

			Fails if:
			- parent does not exist
			- path already exists

			Examples:
			- "/src"
			- "/src/components"
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> createFolder(@ToolParam(description = "Absolute folder path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().createFolder(userId, projectId, path));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// DELETE PATH
	// ---------------------------------------------------------------------

	@Tool(name = "project_delete_path", description = """
			Delete a file or folder.

			Arguments:
			- path: absolute file or folder path

			Behavior:
			- deleting a folder removes the full subtree recursively

			This operation is permanent.

			Fails if:
			- path does not exist
			- path is "/"

			Examples:
			- "/old.txt"
			- "/tmp"
			""")
	@Transactional
	public MintyToolResponse<Integer> deletePath(@ToolParam(description = "Absolute file or folder path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			if ("/".equals(path)) {
				return MintyToolResponse.FailureResponse("Cannot delete root folder.");
			}
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().deleteNode(userId, projectId, path));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// MOVE PATH
	// ---------------------------------------------------------------------

	@Tool(name = "project_move_path", description = """
			Move or rename a file or folder.

			Arguments:
			- sourcePath: existing absolute path
			- targetPath: new absolute path

			Behavior:
			- moving a folder moves the full subtree
			- moved nodes receive new versions

			Fails if:
			- source does not exist
			- target already exists
			- moving a folder inside itself

			Examples:

			Rename file:
			sourcePath="/src/A.java"
			targetPath="/src/B.java"

			Move folder:
			sourcePath="/old"
			targetPath="/archive/old"
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> movePath(@ToolParam(description = "Existing absolute path") String sourcePath,
			@ToolParam(description = "New absolute path") String targetPath) {
		try {
			ensureProjectSelected();
			PathValidator.validate(sourcePath);
			PathValidator.validate(targetPath);
			if (targetPath.startsWith(sourcePath + "/")) {
				return MintyToolResponse.FailureResponse("Cannot move a folder inside itself.");
			}
			return MintyToolResponse.SuccessResponse(
					pluginServices.getProjectService().moveNode(userId, projectId, sourcePath, targetPath));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// SEARCH FILES
	// ---------------------------------------------------------------------

	@Tool(name = "project_search_paths", description = """
			Search files and folders by partial name or path.

			Arguments:
			- filter: partial filename or path fragment

			Behavior:
			- case-insensitive
			- searches the entire project

			Returns:
			- matching files and folders

			Examples:
			- "controller"
			- "README"
			- "service"
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> searchFilesBySubstring(
			@ToolParam(description = "Partial filename or path fragment") String filter) {
		try {
			ensureProjectSelected();
			List<NodeInfo> results = pluginServices.getProjectService().searchByFilter(userId, projectId, filter);
			return MintyToolResponse.SuccessResponse(results);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Error during search: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------

	@Override
	public String name() {
		return "Project Tools";
	}

	@Override
	public String description() {
		return """
				Filesystem tools for reading and modifying
				project files and folders.
				""";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}

	@Override
	public void setConversationId(ConversationId conversationId) {
		this.conversationId = conversationId;
	}

	@Override
	public boolean isPublic() {
		return false;
	}
}