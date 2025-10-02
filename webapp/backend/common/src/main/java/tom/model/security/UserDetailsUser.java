package tom.model.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import tom.api.UserId;
import tom.user.model.User;

public class UserDetailsUser implements UserDetails {

	private static final long serialVersionUID = -1275414448809968650L;

	private String username;
	private String password;
	private UserId userId;

	public UserDetailsUser(User user) {
		this.username = user.getName();
		this.password = user.getPassword();
		this.userId = user.getId();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> auth = new ArrayList<>();
		auth.add(new SimpleGrantedAuthority("ROLE_USER"));
		return auth;
	}

	public UserId getId() {
		return userId;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
