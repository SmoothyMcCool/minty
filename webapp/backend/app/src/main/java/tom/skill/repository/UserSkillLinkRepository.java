package tom.skill.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.skill.model.joins.UserSkillId;
import tom.skill.model.joins.UserSkillLink;

public interface UserSkillLinkRepository extends JpaRepository<UserSkillLink, UserSkillId> {

	List<UserSkillLink> findById_UserIdIn(List<UUID> userIds);

	List<UserSkillLink> findById_SkillId(UUID skillId);

	void deleteById_UserId(UUID userId);

	void deleteById_SkillId(UUID skillId);
}