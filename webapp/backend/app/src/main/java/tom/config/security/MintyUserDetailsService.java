package tom.config.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.model.security.UserDetailsUser;
import tom.user.model.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserServiceInternal;

public class MintyUserDetailsService implements UserDetailsService {

	private static final Logger logger = LogManager.getLogger(MintyUserDetailsService.class);

	private UserRepository userRepository;
	private UserServiceInternal userService;

	public MintyUserDetailsService(UserRepository userRepository, UserServiceInternal userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		logger.info("UserDetailsService: Request for user: " + username);
		User user = null;
		try {
			user = userService.decrypt(userRepository.findByAccount(username));
		} catch (JsonProcessingException e) {
			logger.error("MintyUserDetailsService: Failed to decrypt user", e);
		}
		if (user == null) {
			throw new UsernameNotFoundException("No user with username " + username);
		}
		return new UserDetailsUser(user);
	}

}
