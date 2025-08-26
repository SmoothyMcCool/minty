package tom.api.services;

import java.util.UUID;

public interface UserService {

	final UUID DefaultId = UUID.fromString("00000000-0000-0000-0000-000000000000");

	String getUsernameFromId(UUID userId);

}
