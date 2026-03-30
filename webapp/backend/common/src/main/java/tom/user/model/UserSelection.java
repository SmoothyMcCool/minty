package tom.user.model;

import java.util.List;

public class UserSelection {
	private boolean allUsers;
	private List<String> selectedUsers;

	public UserSelection() {

	}

	public boolean isAllUsers() {
		return allUsers;
	}

	public void setAllUsers(boolean allUsers) {
		this.allUsers = allUsers;
	}

	public List<String> getSelectedUsers() {
		return selectedUsers;
	}

	public void setSelectedUsers(List<String> selectedUsers) {
		this.selectedUsers = selectedUsers;
	}

}
