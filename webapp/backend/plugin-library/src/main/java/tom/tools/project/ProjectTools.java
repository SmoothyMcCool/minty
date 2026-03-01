package tom.tools.project;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tom.api.ProjectId;
import tom.api.UserId;
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
	private ProjectId projectId;

	public static final String prompt = """
			You are operating on a structured project filesystem stored in a database.

			You MUST use tools to inspect and modify the project.
			Never invent file paths.
			Never assume folders exist.
			Always verify structure before modifying it.

			PATH RULES
			- Paths must be absolute.
			- Paths must start with "/".
			- Paths must NOT contain "..".
			- Do not use relative paths.
			- "/" is the root folder and cannot be deleted.

			WORKFLOW RULES

			Before modifying:
			1. Inspect structure using describeProjectStructure or listDirectory.
			2. If editing a file:
			   - First call readNode.
			   - Then produce full updated content.
			   - Then call writeFile with the COMPLETE content.

			WRITE BEHAVIOR
			- writeFile replaces the entire file.
			- writeFile automatically increments version.
			- createFolder fails if parent does not exist.
			- deleteNode deletes folders recursively.
			- moveNode moves entire subtree.

			ERROR HANDLING
			If a tool returns status="error":
			- Read the error message.
			- Adjust the plan.
			- Retry with corrected parameters.
			- Do NOT repeat the same failing call.

			GOAL
			Maintain a consistent, well-structured project tree.
			Avoid duplicates.
			Avoid invalid paths.
			Make deliberate, minimal changes.

						""";

	@Override
	public void initialize() {
		projectId = null;

		Map<String, String> userDefaults = pluginServices.getUserService().getUserDefaults(userId);

		String projectIdStr = userDefaults.getOrDefault("defaultProject", "");

		if (!projectIdStr.isBlank()) {
			projectId = new ProjectId(projectIdStr);
		}
	}

	private void ensureProjectSelected() {
		if (projectId == null) {
			throw new IllegalStateException("No project selected. User has no default project.");
		}
	}

	// ---------------------------------------
	// LIST DIRECTORY
	// ---------------------------------------

	@Tool(description = """
			List the direct children of a folder.

			INPUT:
			- path: Absolute folder path starting with "/".

			BEHAVIOR:
			- Returns only immediate children (not recursive).
			- Fails if path does not exist.
			- Fails if path is not a folder.

			USE THIS:
			- To check whether a folder exists.
			- Before creating files inside a folder.
			- Before structural changes.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> listDirectory(String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);

			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().listChildren(userId, projectId, path));

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------
	// READ NODE (file OR folder)
	// ---------------------------------------

	@Tool(description = """
			Read a node at an absolute path.

			INPUT:
			- path: Absolute path starting with "/".

			RETURNS:
			- If file: returns full file content and version.
			- If folder: returns metadata only (no content).

			FAILS IF:
			- Path does not exist.

			USE THIS:
			- Before modifying a file.
			- To confirm file version.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<NodeContent> readNode(String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);

			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().readNode(userId, projectId, path));

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------
	// WRITE FILE (create or overwrite)
	// ---------------------------------------

	@Tool(description = """
			Create or overwrite a file at an absolute path.

			INPUT:
			- path: Absolute path starting with "/".
			- fileType: One of: code, markdown, json, text, diagram.
			- content: FULL file content (not partial).

			BEHAVIOR:
			- Creates file if it does not exist.
			- Overwrites entire file if it exists.
			- Automatically increments file version.
			- Parent folder MUST already exist.

			FAILS IF:
			- Parent folder does not exist.
			- Path refers to a folder.
			- fileType is invalid.

			IMPORTANT:
			You must send the COMPLETE updated content.
			This does not patch or append.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> writeFile(String path,
			@ToolParam(description = "Must be one of exactly these strings: code, markdown, json, text, diagram") String fileType,
			String content) {

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

	// ---------------------------------------
	// CREATE FOLDER
	// ---------------------------------------

	@Tool(description = """
			Create a folder at an absolute path.

			INPUT:
			- path: Absolute path starting with "/".

			BEHAVIOR:
			- Parent folder must already exist.
			- Version is initialized to 0.

			FAILS IF:
			- Parent does not exist.
			- Path already exists.

			USE THIS:
			- Before writing files into new directories.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> createFolder(String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);

			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().createFolder(userId, projectId, path));

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------
	// DELETE NODE (recursive for folders)
	// ---------------------------------------

	@Tool(description = """
			Delete a file or folder at an absolute path.

			INPUT:
			- path: Absolute path starting with "/".

			BEHAVIOR:
			- If folder: deletes entire subtree recursively.
			- Returns number of deleted nodes.

			FAILS IF:
			- Path does not exist.
			- Path is "/".

			WARNING:
			This operation is permanent.
			""")
	@Transactional
	public MintyToolResponse<Integer> deleteNode(String path) {
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

	// ---------------------------------------
	// MOVE NODE
	// ---------------------------------------

	@Tool(description = """
			Move or rename a file or folder.

			INPUT:
			- sourcePath: Existing absolute path.
			- targetPath: New absolute path.

			BEHAVIOR:
			- Moves entire subtree if folder.
			- Increments version of moved nodes.
			- Target must not already exist.

			FAILS IF:
			- Source does not exist.
			- Target already exists.
			- Attempting to move a folder inside itself.

			USE THIS:
			- To rename files.
			- To reorganize folder structure.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> moveNode(String sourcePath, String targetPath) {

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

	// ---------------------------------------
	// DESCRIBE PROJECT STRUCTURE
	// ---------------------------------------

	@Tool(description = """
			Return the full project tree.

			BEHAVIOR:
			- Returns all nodes ordered by path.
			- Includes node type and version.

			USE THIS:
			- Before large structural changes.
			- When unsure about project layout.
			- To get a global view of the project.

			This is a read-only inspection tool.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> describeProjectStructure() {
		try {
			ensureProjectSelected();

			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().describeTree(userId, projectId));

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------

	@Override
	public String name() {
		return "Project Tools";
	}

	@Override
	public String description() {
		return """
				Tools allowing the LLM to interact with a structured
				project filesystem stored in a database.
				Supports reading, writing, moving, deleting,
				and inspecting files and folders.
				""";
	}

	@Override
	public String prompt() {
		return prompt;
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
