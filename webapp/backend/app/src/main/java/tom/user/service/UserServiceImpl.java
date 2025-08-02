package tom.user.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.user.repository.EncryptedUser;
import tom.user.repository.User;
import tom.user.repository.UserRepository;

@Service
public class UserServiceImpl implements UserServiceInternal {

	private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

	@Value("${secret}")
	private String secret;

	private final UserRepository userRepository;

	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public User decrypt(EncryptedUser encryptedUser) throws JsonMappingException, JsonProcessingException {
		TextEncryptor te = Encryptors.delux(secret, encryptedUser.getSalt());
		String decrypted = te.decrypt(encryptedUser.getCrypt());

		ObjectMapper mapper = new ObjectMapper();
		User user = mapper.readValue(decrypted, User.class);
		user.setPassword(encryptedUser.getPassword());
		user.setId(encryptedUser.getId());
		return user;
	}

	@Override
	public EncryptedUser encrypt(User user) throws JsonProcessingException {
		String salt = KeyGenerators.string().generateKey();
		TextEncryptor te = Encryptors.delux(secret, salt);

		ObjectMapper mapper = new ObjectMapper();
		String password = user.getPassword();
		user.setPassword("");
		String jsonUser = mapper.writeValueAsString(user);
		user.setPassword(password);

		String encrypted = te.encrypt(jsonUser);

		EncryptedUser eu = new EncryptedUser();
		eu.setAccount(user.getName());
		eu.setId(user.getId());
		eu.setPassword(user.getPassword());
		eu.setCrypt(encrypted);
		eu.setSalt(salt);

		return eu;
	}

	@Override
	public String getUsernameFromId(int userId) {
		return userRepository.findById(userId).get().getAccount();
	}

	@Override
	public Optional<User> getUserFromName(String userName) {
		try {
			return Optional.of(decrypt(userRepository.findByAccount(userName)));
		} catch (JsonProcessingException e) {
			logger.warn("Failed to get user for username " + userName);
			return Optional.empty();
		}
	}

	@Override
	public Optional<User> getUserFromId(int userId) {
		try {
			return Optional.of(decrypt(userRepository.findById(userId).get()));
		} catch (JsonProcessingException e) {
			logger.warn("Failed to get user for user ID " + userId);
			return Optional.empty();
		}
	}
}
