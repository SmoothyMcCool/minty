package tom.skill.model.joins;

import java.util.UUID;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import tom.api.UserId;
import tom.skill.model.Skill;

@Entity
@Table(name = "UserSkillsLinks")
public class UserSkillLink {

	@EmbeddedId
	private UserSkillId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("skillId")
	@JoinColumn(name = "skillId", nullable = false)
	private Skill skill;

	public UserSkillLink() {
	}

	public UserSkillLink(UserId userId, Skill skill) {
		this.skill = skill;
		id = new UserSkillId(userId.getValue(), skill.getId());
	}

	public UserSkillId getId() {
		return id;
	}

	public void setId(UserSkillId id) {
		this.id = id;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setSkill(Skill skill) {
		this.skill = skill;
	}
}
