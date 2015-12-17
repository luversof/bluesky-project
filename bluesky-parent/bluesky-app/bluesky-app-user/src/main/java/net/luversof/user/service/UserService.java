package net.luversof.user.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.domain.UserType;
import net.luversof.user.repository.UserRepository;

@Service
@Transactional("securityTransactionManager")
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
		user.setEnable(true);
		user.setUserType(UserType.LOCAL);
		List<UserAuthority> userAuthorityList = new ArrayList<>();
		UserAuthority userAuthority = new UserAuthority();
		userAuthority.setAuthority("ROLE_USER");
		userAuthority.setUser(user);
		userAuthorityList.add(userAuthority);
		user.setUserAuthorityList(userAuthorityList);
		userRepository.save(user);
		return user;
	}

	public User save(User user) {
		return userRepository.save(user);
	}

	@Transactional(value = "securityTransactionManager", readOnly = true)
	public User findOne(long id) {
		return userRepository.findOne(id);
	}

	@Transactional(value = "securityTransactionManager", readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public void remove(User user) {
		userRepository.delete(user);
	}
}
