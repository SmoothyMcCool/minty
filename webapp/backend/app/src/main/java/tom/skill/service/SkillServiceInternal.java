package tom.skill.service;

import tom.api.UserId;
import tom.api.services.SkillService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.user.model.ResourceSharingSelection;
import tom.user.model.UserSelection;

public interface SkillServiceInternal extends SkillService {

	void shareSkill(UserId userId, ResourceSharingSelection selection) throws NotFoundException;

	UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException;
}
