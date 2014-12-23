package net.luversof.user.service;

import java.util.ArrayList;
import java.util.List;

import net.luversof.opensource.jdbc.routing.DataSource;
import net.luversof.opensource.jdbc.routing.DataSourceType;
import net.luversof.user.domain.User;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.MEMBER)
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

	@Transactional(readOnly = true)
	public User findOne(long id) {
		return userRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public void remove(User user) {
		userRepository.delete(user);
	}
}
