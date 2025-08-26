package tom.user.service;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.api.services.UserService;
import tom.user.repository.EncryptedUser;
import tom.user.repository.User;

public interface UserServiceInternal extends UserService {

	public User decrypt(EncryptedUser encryptedUser) throws JsonMappingException, JsonProcessingException;

	public EncryptedUser encrypt(User user) throws JsonProcessingException;

	public String getUsernameFromId(UUID userId);

	public Optional<User> getUserFromName(String userName);

	public Optional<User> getUserFromId(UUID userId);
}
