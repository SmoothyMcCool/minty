package tom.api.model.user;

import java.util.UUID;

import tom.api.UserId;

public class ResourceSharingSelection {

	public static final UserId AllUsersId = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

	private String resource;
	private UserSelection userSelection;

	public ResourceSharingSelection() {
	}

	public ResourceSharingSelection(String resource, UserSelection userSelection) {
		this.resource = resource;
		this.userSelection = userSelection;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public UserSelection getUserSelection() {
		return userSelection;
	}

	public void setUserSelection(UserSelection userSelection) {
		this.userSelection = userSelection;
	}

}
