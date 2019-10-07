package net.luversof.user.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.domain.UserType;
import net.luversof.user.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	/**
	 * 최초 회원 가입 처리
	 * @param username
	 * @param password
	 * @return
	 */
	public User addUser(String username, String password) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		user.setAccountNonExpired(true);
		user.setAccountNonLocked(true);
		user.setCredentialsNonExpired(true);
		user.setEnabled(true);
		UserType userType = UserType.LOCAL;
		user.setUserType(userType);
		List<UserAuthority> userAuthorityList = new ArrayList<>();
		for (String authority : userType.getAuthorities()) {
			UserAuthority userAuthority = new UserAuthority();
			userAuthority.setAuthority(authority);
			userAuthority.setUser(user);
			userAuthorityList.add(userAuthority);
		}
		user.setUserAuthorityList(userAuthorityList);
		userRepository.save(user);
		return user;
	}
	
	/**
	 * 외부인증을 통한 유저 추가의 경우
	 * password는 없으며 enabled를 false로 처리하여 사이트 활성화 여부를 확인 받는다.
	 * @param username
	 * @param userType
	 * @return
	 */
	public User addUser(String username, UserType userType, String externalId, List<String> authorityList) {
		User user = new User();
		user.setUsername(username);
		user.setAccountNonExpired(true);
		user.setAccountNonLocked(true);
		user.setCredentialsNonExpired(true);
		user.setEnabled(false);
		user.setUserType(userType);
		user.setExternalId(externalId);
		List<UserAuthority> userAuthorityList = new ArrayList<>();
		for (String authority : authorityList) {
			UserAuthority userAuthority = new UserAuthority();
			userAuthority.setAuthority(authority);
			userAuthority.setUser(user);
			userAuthorityList.add(userAuthority);
		}
		user.setUserAuthorityList(userAuthorityList);
		userRepository.save(user);
		return user;
	}

	public User save(User user) {
		return userRepository.save(user);
	}

	public User findOne(long id) {
		return userRepository.getOne(id);
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}
	
	public Optional<User> findByExternalIdAndUserType(String externalId, UserType userType) {
		return userRepository.findByExternalIdAndUserType(externalId, userType);
	}

	public void remove(User user) {
		userRepository.delete(user);
	}
}
