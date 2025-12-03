package tom.config.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

import tom.user.repository.UserRepository;
import tom.user.service.UserServiceInternal;

@Configuration
@EnableWebSecurity
@ComponentScan("tom")
public class SecurityConfig {

	@Bean
	BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UserRepository userRepository, UserServiceInternal userService) {
		return new ExerciseTrackerUserDetailsService(userRepository, userService);
	}

	@Bean
	public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder,
			UserDetailsService userDetailsService) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setPasswordEncoder(passwordEncoder);
		provider.setUserDetailsService(userDetailsService);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf((csrf) -> csrf.disable())
			// .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
			.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))

			.authorizeHttpRequests(authorizeRequests -> authorizeRequests

				.requestMatchers(r -> {
					String path = r.getServletPath();
					List<String> extensions = List.of(".js", ".css", ".html", ".map", "ttf", ".woff", ".woff2", ".jpg",
							".png");
					for (String extension : extensions) {
						if (path.endsWith(extension)) {
							return true;
						}
					}
					return false;
				}).permitAll()

				.requestMatchers("/api/user/new", "/api/login").permitAll()

				.requestMatchers("/api/**")	.authenticated()

				.anyRequest().permitAll())
				// .authorizeHttpRequests(authorizeRequests -> authorizeRequests
				// .requestMatchers("/api/user/new",
				// "/api/login").permitAll().anyRequest().authenticated())
				.requestCache(requestCache -> requestCache.requestCache(new NullRequestCache()))

				.httpBasic(httpBasic -> {
				})

				.logout(logout ->
					logout.logoutUrl("/api/logout")
						.logoutSuccessUrl("/login")
				);

		return http.build();

	}
}