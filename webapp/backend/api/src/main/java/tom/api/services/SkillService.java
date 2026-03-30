package tom.api.services;

import java.io.IOException;
import java.util.List;

import tom.api.UserId;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.api.skill.Skill;
import tom.api.skill.SkillMetadata;

public interface SkillService {

	List<SkillMetadata> listSkills(UserId userId);

	String getFile(UserId userId, String skillName, String filename);

	Skill getSkill(UserId userId, String name);

	void removeSkill(UserId userId, String name) throws IOException, NotFoundException, NotOwnedException;

	Skill addSkill(UserId userId, byte[] file) throws IOException, NotOwnedException;

}