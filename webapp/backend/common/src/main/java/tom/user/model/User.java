package tom.user.model;

import java.util.Map;

import tom.api.UserId;

public class User {

	private UserId id;
	private String name;
	private String password;
	private Map<String, String> defaults;
	private String displayMode;

	public UserId getId() {
		return id;
	}

	public void setId(UserId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, String> getDefaults() {
		return defaults;
	}

	public void setDefaults(Map<String, String> defaults) {
		this.defaults = defaults;
	}

	public String getDisplayMode() {
		return displayMode;
	}

	public void setDisplayMode(String displayMode) {
		this.displayMode = displayMode;
	}

}
