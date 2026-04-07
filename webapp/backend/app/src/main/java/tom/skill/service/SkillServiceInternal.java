package tom.skill.service;

import tom.api.UserId;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.services.SkillService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;

public interface SkillServiceInternal extends SkillService {

	void shareSkill(UserId userId, ResourceSharingSelection selection) throws NotFoundException;

	UserSelection getSharingFor(UserId userId, String name) throws NotOwnedException, NotFoundException;
}
