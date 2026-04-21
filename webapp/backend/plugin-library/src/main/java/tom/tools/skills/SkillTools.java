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

@Component
public class SkillTools implements MintyTool, ServiceConsumer {

	private PluginServices pluginServices;
	private UserId userId;

	public static final String prompt = """
			## Skills

			You have access to a skill system that gives you step-by-step instructions for complex tasks.

			How to use skills:
			1. Call list_skills to see what skills are available and what each one does.
			2. When a skill matches the user's request, call get_skill with the skill name to read its instructions.
			3. Follow the instructions in SKILL.md exactly and in order.
			4. If SKILL.md tells you to read another file, call get_skill_file with the exact filename it specifies.
			5. Only load one file at a time. Do not load the next file until the current step is complete.
			6. Do not skip steps. Do not read ahead.

			Important rules:
			- Always check list_skills before attempting a complex task - a skill may exist for it.
			- Never guess at filenames. Only request files that are explicitly named in instructions you have already read.
			- If a skill step tells you to stop and wait for the user, do so before loading the next file.

									""";

	@Tool(name = "list_skills", description = """
			Returns the name and description of every skill that is available.
			Call this first to discover which skills exist before trying to use one.
			Each skill has a name (used to load it) and a description (tells you what it does and when to use it).
			""")
	public List<SkillMetadata> listSkills() {
		return pluginServices.getSkillsService().listSkills(userId);
	}

	@Tool(name = "get_skill", description = """
			Returns the full contents of SKILL.md for the named skill.
			Call this after list_skills once you have identified the skill you need.
			SKILL.md contains the instructions you must follow to complete the task.
			It may also tell you to load additional files using get_skill_file - follow those instructions exactly.
			""")
	public String getSkill(
			@ToolParam(description = "The exact skill name as returned by list_skills.") String skillName) {
		return pluginServices.getSkillsService().getFile(userId, skillName, "SKILL.md");
	}

	@Tool(name = "get_skill_file", description = """
			Returns the contents of a supporting file that belongs to a skill.
			Only call this when SKILL.md explicitly tells you to load a specific file.
			Use the exact filename path as written in SKILL.md (for example: steps/step-1-collect.md).
			Do not load files speculatively - only load the file that the current instruction refers to.
			""")
	public String getSkillFile(
			@ToolParam(description = "The exact skill name as returned by list_skills.") String skillName,
			@ToolParam(description = "The relative file path exactly as referenced in SKILL.md, for example: steps/step-1-collect.md") String filename) {
		return pluginServices.getSkillsService().getFile(userId, skillName, filename);
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

	@Override
	public String name() {
		return "Skill Tools";
	}

	@Override
	public String description() {
		return "Tools that allow the LLM to call skills.";
	}
}
