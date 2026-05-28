package tom.tools.skills;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import tom.api.UserId;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.skill.SkillMetadata;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;

@Component
public class SkillTools implements MintyTool, ServiceConsumer {

	private PluginServices pluginServices;
	private UserId userId;

	// ---------------------------------------------------------------------
	// LIST SKILLS
	// ---------------------------------------------------------------------

	@Tool(name = "skills_list", description = """
			List all available skills.

			Returns:
			- skill names
			- skill descriptions

			Use this to discover available skills.
			""")
	public MintyToolResponse<List<SkillMetadata>> listSkills() {
		try {
			return MintyToolResponse.SuccessResponse(pluginServices.getSkillsService().listSkills(userId));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// GET SKILL
	// ---------------------------------------------------------------------

	@Tool(name = "skills_get_instruction", description = """
			Load the main instructions for a skill.

			Arguments:
			- skillName: exact skill name

			Returns:
			- contents of SKILL.md

			SKILL.md may reference additional files
			that can be loaded with skills_get_file.

			Fails if:
			- skill does not exist
			""")
	public MintyToolResponse<String> getSkill(@ToolParam(description = "Exact skill name") String skillName) {
		try {
			return MintyToolResponse
					.SuccessResponse(pluginServices.getSkillsService().getFile(userId, skillName, "SKILL.md"));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// GET SKILL FILE
	// ---------------------------------------------------------------------

	@Tool(name = "skills_get_file", description = """
			Load a supporting file from a skill.

			Arguments:
			- skillName: exact skill name
			- filename: relative file path inside the skill

			Use this only for files referenced
			by SKILL.md.

			Examples:
			- "steps/step-1.md"
			- "templates/review.md"

			Fails if:
			- skill does not exist
			- file does not exist
			""")
	public MintyToolResponse<String> getSkillFile(@ToolParam(description = "Exact skill name") String skillName,
			@ToolParam(description = "Relative file path inside the skill") String filename) {
		try {
			return MintyToolResponse
					.SuccessResponse(pluginServices.getSkillsService().getFile(userId, skillName, filename));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------

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
		return "Skill Tools";
	}

	@Override
	public String description() {
		return "Tools for discovering and loading reusable skills.";
	}
}