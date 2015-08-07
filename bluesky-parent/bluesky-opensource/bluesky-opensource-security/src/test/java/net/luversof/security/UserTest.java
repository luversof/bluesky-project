package net.luversof.security;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
public class UserTest extends GeneralTest {

	@Autowired
	private UserService userService;
	
	@Test
	public void 테스트() {
//		User user = userService.findByUsername("bluesky");
//		log.debug("user : {}", user);
		log.debug("Test");
	}
	
	@Test
	public void 비밀번호암호화테스트() {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		System.out.println(bCryptPasswordEncoder.encode("bluesky"));
	}
	
	@Test
	public void 회원가입() {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		User user = userService.addUser("bluesky", bCryptPasswordEncoder.encode("bluesky"));
		log.debug("user : {}", user);
	}
}
