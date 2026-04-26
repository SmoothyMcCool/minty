package tom.tools.agents;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;

@Component
public class AgentTools implements MintyTool, ServiceConsumer {

	private PluginServices pluginServices;

	@Tool(name = "get_agent_definition", description = """
			Returns the full definition for a named agent including all whenToUse,
			whenNotToUse, input specifications, and output description.
			Call this before using any dynamic agent in a step.
				""")
	public String getAgentDefinition(@ToolParam(description = "The name of the agent.") String agentName) {
		return pluginServices.getAgentRegistry().getAgentDescription(agentName);
	}

	@Override
	public String prompt() {
		return "";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public String name() {
		return "Agent Tools";
	}

	@Override
	public String description() {
		return "These are internal tools that allow planning agents to discover what agents are available.";
	}
}
