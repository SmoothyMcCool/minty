package tom.config.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.user.repository.User;
import tom.user.repository.UserRepository;
import tom.user.service.UserService;

public class ExerciseTrackerUserDetailsService implements UserDetailsService {

	private static final Logger logger = LogManager.getLogger(ExerciseTrackerUserDetailsService.class);

	private UserRepository userRepository;
	private UserService userService;

	public ExerciseTrackerUserDetailsService(UserRepository userRepository, UserService userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		System.out.println("UserDetailsService: Request for user: " + username);
		User user = null;
		try {
			user = userService.decrypt(userRepository.findByAccount(username));
		} catch (JsonProcessingException e) {
			logger.error("ExerciseTrackerUserDetailsService: Failed to decrypt user");
		}
		return user == null ? null : new UserDetailsUser(user);
	}

}
