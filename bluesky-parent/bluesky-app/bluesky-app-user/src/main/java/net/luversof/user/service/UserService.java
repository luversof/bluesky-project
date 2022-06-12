package net.luversof.user.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.domain.User;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.domain.UserType;
import net.luversof.user.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserAuthorityService userAuthorityService;
	
	public User addUser(@BlueskyValidated(User.Create.class) User user) {
		userRepository.findByUserName(user.getUserName()).ifPresent(target -> {
			throw new BlueskyException(UserErrorCode.ALREADY_EXIST_USER);
		});
		
		user.setUserId(UUID.randomUUID().toString());
		user.setAccountNonExpired(true);
		user.setAccountNonLocked(true);
		user.setCredentialsNonExpired(true);
		user.setEnabled(true);
		UserType userType = UserType.LOCAL;
		user.setUserType(userType);
		userRepository.save(user);
		
		List<UserAuthority> userAuthorityList = new ArrayList<>();
		for (String authority : userType.getAuthorities()) {
			UserAuthority userAuthority = new UserAuthority();
			userAuthority.setUserId(user.getUserId());
			userAuthority.setAuthority(authority);
			userAuthorityList.add(userAuthority);
		}
		userAuthorityService.saveAll(userAuthorityList);
		user.setUserAuthorityList(userAuthorityList);
		return user;
	}

	/**
	 * 최초 회원 가입 처리
	 * @param username
	 * @param password
	 * @return
	 */
	public User addUser(String username, String password) {
		User user = new User();
		user.setUserName(username);
		user.setPassword(password);
		return addUser(user);
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
		user.setUserId(UUID.randomUUID().toString());
		user.setUserName(username);
		user.setAccountNonExpired(true);
		user.setAccountNonLocked(true);
		user.setCredentialsNonExpired(true);
		user.setEnabled(false);
		user.setUserType(userType);
		user.setExternalId(externalId);
		List<UserAuthority> userAuthorityList = new ArrayList<>();
		for (String authority : authorityList) {
			UserAuthority userAuthority = new UserAuthority();
			userAuthority.setUserId(user.getUserId());
			userAuthority.setAuthority(authority);
			userAuthorityList.add(userAuthority);
		}
		user.setUserAuthorityList(userAuthorityList);
		userRepository.save(user);
		return user;
	}

	public User save(User user) {
		return userRepository.save(user);
	}

	public Optional<User> findByUserId(String userId) {
		return userRepository.findByUserId(userId);
	}
	
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUserName(username);
	}
	
	public Optional<User> findByExternalIdAndUserType(String externalId, UserType userType) {
		return userRepository.findByExternalIdAndUserType(externalId, userType);
	}

	public void remove(User user) {
		userRepository.delete(user);
	}
}
