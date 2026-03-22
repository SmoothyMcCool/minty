package tom.api.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import tom.api.skill.Skill;
import tom.api.skill.SkillMetadata;

public interface SkillService {

	List<SkillMetadata> listSkills();

	String getFile(String skillName, String filename);

	Skill getSkill(String name);

	boolean validate(Path skillFile) throws IOException;

	void addSkill(Path skillFile) throws IOException;
}