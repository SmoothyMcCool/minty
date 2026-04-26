package tom.tools.workflow;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import tom.api.UserId;
import tom.api.WorkflowId;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.services.exception.NotOwnedException;
import tom.api.services.workflow.Workflow;
import tom.api.services.workflow.WorkflowDescription;
import tom.api.tool.MintyTool;

@Component
public class WorkflowTools implements MintyTool, ServiceConsumer {

	private PluginServices pluginServices;
	private UserId userId;

	public static final String prompt = """
			## Workflows
			You can create, manage, and run workflows using the workflow tools.
			Workflows are data pipelines made up of steps (tasks) connected together.

			How to build and run a workflow:
			1. Use the workflow-builder skill to generate the workflow JSON from a plain-English description.
			   Call list_skills and load the workflow-builder skill before writing any workflow JSON.
			2. Call create_workflow with the generated JSON to save the workflow.
			3. Call execute_workflow with a WorkflowRequest to run it.
			4. Call list_workflows to see all existing workflows.
			5. Call get_workflow to inspect a specific workflow by ID.
			6. Call update_workflow to modify an existing workflow.
			7. Call delete_workflow to remove a workflow permanently.
			8. Call cancel_workflow to stop a running workflow by name.

			Important rules:
			- Always use the workflow-builder skill to produce workflow JSON - do not write it from memory.
			- Never reuse step IDs in the same workflow. Every step must have a unique UUID.
			- The workflow JSON must be valid before calling create_workflow or update_workflow.
			  Use the generation checklist in workflow-schema.md to verify it first.
			- execute_workflow runs the workflow asynchronously. The returned execution ID can be
			  used to track progress.
			""";

	@Tool(name = "list_workflows", description = """
			Returns a list of all workflows belonging to the current user.
			Each entry includes the workflow ID, name, and description.
			Call this to discover existing workflows before creating a duplicate,
			or to find a workflow ID for get_workflow, execute_workflow, or delete_workflow.
			""")
	public List<WorkflowDescription> listWorkflows() {
		return pluginServices.getWorkflowService().listWorkflows(userId);
	}

	@Tool(name = "get_workflow", description = """
			Returns the full definition of a single workflow, including all steps,
			connections, and configuration.
			Use this to inspect an existing workflow before modifying it,
			or to verify a workflow was created correctly.
			""")
	public Workflow getWorkflow(
			@ToolParam(description = "The UUID of the workflow to retrieve, as returned by list_workflows or create_workflow.") String workflowId) {
		return pluginServices.getWorkflowService().getWorkflow(userId, new WorkflowId(workflowId));
	}

	@Tool(name = "create_workflow", description = """
			Saves a new workflow definition and returns the saved workflow including its assigned ID.
			The workflow JSON must be generated using the workflow-builder skill before calling this.
			Use update_workflow to modify an existing workflow instead of creating a duplicate.
			""")
	public String createWorkflow(
			@ToolParam(description = "The complete workflow JSON as a string, generated using the workflow-builder skill.") String workflowJson) {
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			Workflow workflow = mapper.readValue(workflowJson, Workflow.class);
			Workflow created = pluginServices.getWorkflowService().createWorkflow(userId, workflow);
			return mapper.writeValueAsString(created);
		} catch (NotOwnedException e) {
			return "Error: you do not have permission to create this workflow.";
		} catch (Exception e) {
			return "Error creating workflow: " + e.getMessage();
		}
	}

	@Tool(name = "update_workflow", description = """
			Replaces an existing workflow definition with a new one.
			The workflow JSON must include the existing workflow ID.
			Use this to modify steps, connections, or configuration of a workflow you have already created.
			""")
	public String updateWorkflow(
			@ToolParam(description = "The complete updated workflow JSON as a string. Must include the existing workflow ID.") String workflowJson) {
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			Workflow workflow = mapper.readValue(workflowJson, Workflow.class);
			Workflow updated = pluginServices.getWorkflowService().updateWorkflow(userId, workflow);
			return mapper.writeValueAsString(updated);
		} catch (NotOwnedException e) {
			return "Error: you do not have permission to update this workflow.";
		} catch (Exception e) {
			return "Error updating workflow: " + e.getMessage();
		}
	}

	@Tool(name = "delete_workflow", description = """
			Permanently deletes a workflow. This cannot be undone.
			Confirm with the user before calling this.
			""")
	public String deleteWorkflow(
			@ToolParam(description = "The UUID of the workflow to delete, as returned by list_workflows or create_workflow.") String workflowId) {
		try {
			pluginServices.getWorkflowService().deleteWorkflow(userId, new WorkflowId(workflowId));
			return "Workflow " + workflowId + " deleted successfully.";
		} catch (NotOwnedException e) {
			return "Error: you do not have permission to delete this workflow.";
		} catch (Exception e) {
			return "Error deleting workflow: " + e.getMessage();
		}
	}

	/*
	 * @Tool(name = "execute_workflow", description = """ Runs a workflow and
	 * returns an execution ID. The workflow must already exist - call
	 * create_workflow first if it does not. Execution runs asynchronously; use the
	 * returned execution ID to track progress. """) public String executeWorkflow(
	 *
	 * @ToolParam(description =
	 * "The UUID of the workflow to execute, as returned by list_workflows or create_workflow."
	 * ) String workflowId,
	 *
	 * @ToolParam(description =
	 * "Optional name to identify this execution run. Leave blank to use the workflow name."
	 * ) String executionName) { try { WorkflowRequest request = new
	 * WorkflowRequest(); request.setId(UUID.fromString(workflowId)); if
	 * (executionName != null && !executionName.isBlank()) {
	 * request.setName(executionName); } String executionId =
	 * pluginServices.getWorkflowService().executeWorkflow(userId, request); return
	 * "Workflow execution started. Execution ID: " + executionId; } catch
	 * (NotOwnedException e) { return
	 * "Error: you do not have permission to execute this workflow."; } catch
	 * (Exception e) { return "Error executing workflow: " + e.getMessage(); } }
	 *
	 * @Tool(name = "cancel_workflow", description = """ Cancels a currently running
	 * workflow execution by name. Use this to stop a workflow that is taking too
	 * long or was started by mistake. """) public String cancelWorkflow(
	 *
	 * @ToolParam(description =
	 * "The name of the running workflow execution to cancel.") String
	 * executionName) { try {
	 * pluginServices.getWorkflowService().cancelWorkflow(userId, executionName);
	 * return "Workflow execution '" + executionName + "' cancelled successfully.";
	 * } catch (NotOwnedException e) { return
	 * "Error: you do not have permission to cancel this workflow."; } catch
	 * (Exception e) { return "Error cancelling workflow: " + e.getMessage(); } }
	 */

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

	@Override
	public String name() {
		return "Workflow Tools";
	}

	@Override
	public String description() {
		return "Tools that allow the LLM to create, manage, and execute workflows.";
	}

}