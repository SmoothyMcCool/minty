package tom.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import tom.api.MintyObjectMapper;
import tom.api.UserId;
import tom.api.services.UserService;
import tom.config.MintyConfiguration;
import tom.user.model.User;
import tom.user.repository.EncryptedUser;
import tom.user.repository.UserRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

@Service
public class UserServiceImpl implements UserServiceInternal {

	private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

	private final String secret;
	private final UserRepository userRepository;
	private List<String> userNames;
	private final ObjectMapper mapper;

	public UserServiceImpl(UserRepository userRepository, MintyConfiguration properties) {
		this.userRepository = userRepository;
		secret = properties.getConfig().secret();
		userNames = null;
		mapper = MintyObjectMapper.StandardJsonMapper;
	}

	@Override
	public User decrypt(EncryptedUser encryptedUser) throws DatabindException, JacksonException {
		TextEncryptor te = Encryptors.delux(secret, encryptedUser.getSalt());
		String decrypted = te.decrypt(encryptedUser.getCrypt());

		User user = mapper.readValue(decrypted, User.class);
		user.setPassword(encryptedUser.getPassword());
		user.setId(encryptedUser.getId());
		if (user.getDefaults() == null) {
			user.setDefaults(new HashMap<>());
		}
		if (user.getSettings() == null) {
			user.setSettings(defaultSettings());
		}
		return user;
	}

	private Map<String, String> defaultSettings() {
		Map<String, String> defaults = new HashMap<>();
		defaults.put("Message Order", "NewestFirst");
		defaults.put("Button Alignment", "Left");
		defaults.put("Theme", "Light Mode");
		return defaults;
	}

	@Override
	public EncryptedUser encrypt(User user) throws JacksonException {
		String salt = KeyGenerators.string().generateKey();
		TextEncryptor te = Encryptors.delux(secret, salt);

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
	public String getUsernameFromId(UserId userId) {
		return userRepository.findById(userId.value()).get().getAccount();
	}

	@Override
	public Optional<User> getUserFromName(String userName) {
		try {
			return Optional.of(decrypt(userRepository.findByAccount(userName)));
		} catch (JacksonException e) {
			logger.warn("Failed to get user for username " + userName);
			return Optional.empty();
		}
	}

	@Override
	public Optional<User> getUserFromId(UserId userId) {
		if (userId.equals(UserService.DefaultId)) {
			return Optional.of(new User());
		}
		try {
			return Optional.of(decrypt(userRepository.findById(userId.value()).get()));
		} catch (Exception e) {
			logger.warn("Failed to get user for user ID " + userId);
			return Optional.empty();
		}
	}

	@Override
	public Map<String, String> getUserDefaults(UserId userId) {
		User user = getUserFromId(userId).orElse(null);

		if (user != null) {
			return user.getDefaults();
		}

		return null;
	}

	@Override
	public List<String> listUsers() {
		if (userNames != null) {
			return userNames;
		}

		List<EncryptedUser> encryptedUsers = userRepository.findAll();
		List<String> userNames = encryptedUsers.stream().map(user -> user.getAccount()).toList();
		this.userNames = userNames;
		return userNames;
	}

	@Override
	public void invalidateUserList() {
		this.userNames = null;
	}
}
