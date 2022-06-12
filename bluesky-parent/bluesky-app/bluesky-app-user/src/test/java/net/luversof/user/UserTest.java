package net.luversof.user;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;
import net.luversof.user.util.UserUtil;

@Slf4j
@Transactional
class UserTest extends GeneralTest {
	
	private static final String USERNAME = "testUserName";

	@Autowired
	private UserService userService;
	
	@Test
	@DisplayName("유저 조회")
	void findByUsername() {
		User user = userService.findByUsername(USERNAME).get();
		log.debug("user : {}", user);
	}
	
	@Test
	@DisplayName("유저 추가")
	void addUserTest() {
		assertThrows(BlueskyException.class, () ->{
			var user = userService.addUser(USERNAME, "testPassword");
			log.debug("user : {}", user);
		});
	}
	
	@Test
	void 회원삭제() {
		User user = userService.findByUsername(USERNAME).get();
		userService.remove(user);
		log.debug("user : {}", user);
	}
	
//	@Test
//	void 회원가입() {
//		User user = userService.addUser(USERNAME, "$2a$10$F5q9vfYTrF0ZM/ZOUIlPvu2KWrOBcCtP9eM8mnGdbiRoSKRJ7VPwW");
//		log.debug("user : {}", user);
//	}
	
	@Test
	void collect테스트() {
		List<User> userList = new ArrayList<>();
		for (int i = 0 ; i < 100 ; i ++) {
			User user = new User();
			user.setPassword("pas" + i);
			user.setUserName("usr" + i);
			userList.add(user);
		}
		
		log.debug("result : {}", userList);
		List<String> nameList = userList.stream().map(user -> user.getUserName()).collect(Collectors.toList());
		log.debug("result : {}", nameList);
	}
	
	@Test
	void userUtilTest() {
		Optional<User> loginUser = UserUtil.getLoginUser();
		log.debug("user : {}", loginUser);
	}
	
	@Test
	void uuidTest() {
		log.debug("uuid length : {}", UUID.randomUUID().toString().length());
	}
}
