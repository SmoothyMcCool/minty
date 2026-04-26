package tom.skill.model.joins;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserSkillId implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 304838745383109854L;

	@Column(name = "userId")
	private UUID userId;

	@Column(name = "skillId")
	private UUID skillId;

	public UserSkillId() {
	}

	public UserSkillId(UUID userId, UUID skillId) {
		this.userId = userId;
		this.skillId = skillId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getSkillId() {
		return skillId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public void setSkillId(UUID skillId) {
		this.skillId = skillId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UserSkillId))
			return false;
		UserSkillId that = (UserSkillId) o;
		return Objects.equals(userId, that.userId) && Objects.equals(skillId, that.skillId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, skillId);
	}
}