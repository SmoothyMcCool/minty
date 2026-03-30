package tom.skill.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.skill.model.Skill;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

	List<Skill> findByOwnerId(UUID ownerId);

	List<Skill> findByNameContainingIgnoreCase(String name);

	Skill findByName(String name);
}