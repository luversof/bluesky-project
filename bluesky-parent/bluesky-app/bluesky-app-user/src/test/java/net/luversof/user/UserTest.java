package net.luversof.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.user.domain.User;
import net.luversof.user.repository.UserRepository;
import net.luversof.user.service.UserService;
import net.luversof.user.util.UserUtil;

@Slf4j
class UserTest extends GeneralTest {
	
	private static final String USERNAME = "luversof";

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private HibernateProperties hibernateProperties;
	
	@Test
	void 테스트() {
		User user = userService.findByUsername(USERNAME).get();
		log.debug("user : {}", user);
	}
	
	@Test
	void addUserTest() {
		var user = userService.addUser("testUserName", "testPassword");
		assertThat(user).isNotNull();
		log.debug("user : {}", user);
	}
	
	@Test
	void 회원삭제() {
		User user = userService.findByUsername(USERNAME).get();
		userService.remove(user);
		log.debug("user : {}", user);
	}
	
	
	@Test
	void 회원가입() {
		User user = userService.addUser(USERNAME, "$2a$10$F5q9vfYTrF0ZM/ZOUIlPvu2KWrOBcCtP9eM8mnGdbiRoSKRJ7VPwW");
		log.debug("user : {}", user);
	}
	
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
	void findAll() {
		log.debug("result : {}", userRepository.findAll());
	}
	
	@Test
	void findByUserId() {
		log.debug("result : {}", userRepository.findByUserId("2dc8d01e-01bc-474c-a15b-f576ae3bec0a"));
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
