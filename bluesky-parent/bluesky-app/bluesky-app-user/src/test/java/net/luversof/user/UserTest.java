package net.luversof.user;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class UserTest extends GeneralTest {
	
	private static final String USERNAME = "bluesky";

	@Autowired
	private UserService userService;
	
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
}
