package tom.api.services;

import java.util.Map;
import java.util.UUID;

import tom.api.UserId;

public interface UserService {

	final UserId DefaultId = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

	String getUsernameFromId(UserId userId);

	Map<String, String> getUserDefaults(UserId userId);
}
