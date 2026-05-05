package tom.user.service;

import java.util.List;
import java.util.Optional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;

import tom.api.UserId;
import tom.api.services.UserService;
import tom.user.model.User;
import tom.user.repository.EncryptedUser;

public interface UserServiceInternal extends UserService {

	public User decrypt(EncryptedUser encryptedUser) throws DatabindException, JacksonException;

	public EncryptedUser encrypt(User user) throws JacksonException;

	public Optional<User> getUserFromName(String userName);

	public Optional<User> getUserFromId(UserId userId);

	public List<String> listUsers();

	public void invalidateUserList();
}
