package tom.user.model;

import java.util.Map;
import java.util.UUID;

public class User {

	private UUID id;
	private String name;
	private String password;
	private Map<String, String> defaults;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
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

}
