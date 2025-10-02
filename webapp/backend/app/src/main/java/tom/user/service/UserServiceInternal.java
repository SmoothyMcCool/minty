package tom.user.service;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.api.UserId;
import tom.api.services.UserService;
import tom.user.model.User;
import tom.user.repository.EncryptedUser;

public interface UserServiceInternal extends UserService {

	public User decrypt(EncryptedUser encryptedUser) throws JsonMappingException, JsonProcessingException;

	public EncryptedUser encrypt(User user) throws JsonProcessingException;

	public Optional<User> getUserFromName(String userName);

	public Optional<User> getUserFromId(UserId userId);
}
