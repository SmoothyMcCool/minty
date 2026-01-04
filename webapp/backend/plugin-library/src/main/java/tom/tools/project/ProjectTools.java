package tom.tools.project;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.ProjectEntry;
import tom.api.model.project.ProjectEntryInfo;
import tom.api.model.project.ProjectEntryType;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;

public class ProjectTools implements MintyTool, ServiceConsumer {

	private static final Logger logger = LogManager.getLogger(ProjectTools.class);

	private PluginServices pluginServices;
	private UserId userId;

	@Tool(name = "validate_project_exists", description = "Checks if the project exists")
	MintyToolResponse<Boolean> projectExists(@ToolParam() String projectId) {
		boolean exists = false;
		try {
			pluginServices.getProjectService().getProject(userId, new ProjectId(projectId));
			logger.info("Telling the LLM the project exists.");
			exists = true;
		} catch (Exception e) {
			logger.info("Telling the LLM the project does not exist.");
		}
		return MintyToolResponse.SuccessResponse(exists);
	}

	@Tool(name = "list_files", description = "List all files in the project")
	MintyToolResponse<List<String>> listFiles(@ToolParam() String projectId) {
		List<String> response;

		try {
			List<ProjectEntryInfo> entries = pluginServices.getProjectService().listProjectEntries(userId,
					new ProjectId(projectId));
			logger.info("list_files returning: " + entries.stream().map(entry -> entry.getName()).toList().toString());
			if (entries.isEmpty()) {
				response = List.of("This project doesn't contain any files yet.");
			}
			response = entries.stream().map(entry -> entry.getName()).toList();
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId;
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}

		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "list_requirement_documents", description = "List all files in the project that store requirements.")
	MintyToolResponse<List<String>> listRequirementDocuments(@ToolParam() String projectId) {
		List<String> response;
		List<ProjectEntryInfo> entries;

		try {
			entries = listFilesOfType(userId, new ProjectId(projectId), ProjectEntryType.RequirementsDoc);
			logger.info("list_requirement_documents returning: "
					+ entries.stream().map(entry -> entry.getName()).toList().toString());
			response = entries.stream().map(entry -> entry.getName()).toList();
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId;
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}

		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "list_design_documents", description = "List all files in the project that store system design, software design, and architecture.")
	MintyToolResponse<List<String>> listDesignDocuments(@ToolParam() String projectId) {
		List<String> response;
		List<ProjectEntryInfo> entries;

		try {
			entries = listFilesOfType(userId, new ProjectId(projectId), ProjectEntryType.DesignDoc);
			logger.info("list_design_documents returning: "
					+ entries.stream().map(entry -> entry.getName()).toList().toString());
			response = entries.stream().map(entry -> entry.getName()).toList();
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId;
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}

		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "list_user_stories", description = "List all files in the project that store Agile stories for development teams.")
	MintyToolResponse<List<String>> listStoryFiles(@ToolParam() String projectId) {
		List<String> response;
		List<ProjectEntryInfo> entries;

		try {
			entries = listFilesOfType(userId, new ProjectId(projectId), ProjectEntryType.Story);
			logger.info("list_user_stories returning: "
					+ entries.stream().map(entry -> entry.getName()).toList().toString());
			response = entries.stream().map(entry -> entry.getName()).toList();
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId;
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}

		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "list_code_files", description = "List all files in the project that contain code implementation.")
	MintyToolResponse<List<String>> listCodeFiles(@ToolParam() String projectId) {
		List<String> response;
		List<ProjectEntryInfo> entries;

		try {
			entries = listFilesOfType(userId, new ProjectId(projectId), ProjectEntryType.File);
			logger.info(
					"list_code_files returning: " + entries.stream().map(entry -> entry.getName()).toList().toString());
			response = entries.stream().map(entry -> entry.getName()).toList();
		} catch (Exception e) {
			String error = "Failed to list files for project " + projectId;
			logger.warn(error);
			return MintyToolResponse.FailureResponse(error);
		}

		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "get_file_contents", description = "Get the contents of a specific file")
	MintyToolResponse<String> getFileContents(@ToolParam() String projectId, @ToolParam() String fileName) {
		String response;
		ProjectEntry entry = pluginServices.getProjectService().getProjectEntryByName(userId, new ProjectId(projectId),
				fileName);

		if (entry == null) {
			response = "Could not retrieve file.";
			return MintyToolResponse.FailureResponse(response);
		} else {
			response = entry.toString();
		}

		logger.info("get_file_contents returning: " + response);
		return MintyToolResponse.SuccessResponse(response);
	}

	@Tool(name = "create_or_update_requirement_document", description = "Create a file or replace the contents of an existing requirements document")
	MintyToolResponse<String> createOrUpdateRequirementsDocument(@ToolParam() String projectId,
			@ToolParam() String fileName, @ToolParam() String fileContents) {
		return createOrUpdateFileOfType(userId, new ProjectId(projectId), ProjectEntryType.RequirementsDoc, fileName,
				fileContents);
	}

	@Tool(name = "create_or_update_design_document", description = "Create a file or replace the contents of an existing system design, software design, or architecture document")
	MintyToolResponse<String> createOrUpdateDesignDocument(@ToolParam() String projectId, @ToolParam() String fileName,
			@ToolParam() String fileContents) {
		return createOrUpdateFileOfType(userId, new ProjectId(projectId), ProjectEntryType.DesignDoc, fileName,
				fileContents);
	}

	@Tool(name = "create_or_update_code_file", description = "Create a file or replace the contents of an existing code file")
	MintyToolResponse<String> createOrUpdateCodeFile(@ToolParam() String projectId, @ToolParam() String fileName,
			@ToolParam() String fileContents) {
		return createOrUpdateFileOfType(userId, new ProjectId(projectId), ProjectEntryType.File, fileName,
				fileContents);
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

	private List<ProjectEntryInfo> listFilesOfType(UserId userId, ProjectId projectId, ProjectEntryType type)
			throws IOException {
		List<ProjectEntryInfo> entries = pluginServices.getProjectService().listProjectEntries(userId, projectId);
		return entries.stream().filter(entry -> entry.getType().equals(type)).toList();
	}

	private MintyToolResponse<String> createOrUpdateFileOfType(UserId userId, ProjectId projectId,
			ProjectEntryType type, String fileName, String fileContents) {
		ProjectEntry entry = pluginServices.getProjectService().getProjectEntryByName(userId, projectId, fileName);
		ProjectEntryInfo entryInfo;

		if (entry != null) {
			entryInfo = entry.info();
		} else {
			entryInfo = new ProjectEntryInfo();
			entryInfo.setName(fileName);
			entryInfo.setParent(null);
			entryInfo.setType(type);
		}

		ProjectEntry updated = new ProjectEntry(entryInfo, fileContents);
		try {
			pluginServices.getProjectService().createOrUpdateProjectEntry(userId, projectId, updated);
			logger.info("createOrUpdateFileOfType updated " + fileName);
		} catch (Exception e) {
			logger.warn("Failed to update file {}  project {}.", fileName, projectId);
			return MintyToolResponse.FailureResponse("Failed to update file.");
		}
		return MintyToolResponse.SuccessResponse("Successfully updated file.");
	}
}
