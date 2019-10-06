package net.luversof.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.user.domain.User;
import net.luversof.user.repository.UserRepository;
import net.luversof.user.service.UserService;
import net.luversof.user.util.UserUtil;

@Slf4j
public class UserTest extends GeneralTest {
	
	private static final String USERNAME = "luversof";

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Test
	public void 테스트() {
		User user = userService.findByUsername(USERNAME);
		log.debug("user : {}", user);
	}
	
	@Test
	public void 회원삭제() {
		User user = userService.findByUsername(USERNAME);
		userService.remove(user);
		log.debug("user : {}", user);
	}
	
	
	@Test
	public void 회원가입() {
		User user = userService.addUser(USERNAME, "$2a$10$F5q9vfYTrF0ZM/ZOUIlPvu2KWrOBcCtP9eM8mnGdbiRoSKRJ7VPwW");
		log.debug("user : {}", user);
	}
	
	@Test
	public void collect테스트() {
		List<User> userList = new ArrayList<>();
		for (int i = 0 ; i < 100 ; i ++) {
			User user = new User();
			user.setPassword("pas" + i);
			user.setUsername("usr" + i);
			userList.add(user);
		}
		
		log.debug("result : {}", userList);
		List<String> nameList = userList.stream().map(user -> user.getUsername()).collect(Collectors.toList());
		log.debug("result : {}", nameList);
	}
	
	@Test
	public void findAll() {
		log.debug("result : {}", userRepository.findAll());
	}
	
	@Test
	public void userUtilTest() {
		Optional<User> loginUser = UserUtil.getLoginUser();
		log.debug("user : {}", loginUser);
	}
}
