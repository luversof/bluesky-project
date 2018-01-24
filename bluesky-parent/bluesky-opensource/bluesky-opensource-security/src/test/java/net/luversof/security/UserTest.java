package net.luversof.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.security.UserTest.Person.PersonBuilder;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

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
	
	@Test
	public void 암호화테스트() {
		log.debug("test : {}", Hex.encode("cloud".getBytes()));
		log.debug("test : {}", Hex.decode("73616c74"));
		TextEncryptor textEncryptor = Encryptors.text("bluesky", "73616c74");
		log.debug("encrypt:  {}", textEncryptor.encrypt("테스트 문구입니다."));
	}
	
	
	@Test
	public void java8테스트() {
		PersonBuilder builder = Person.builder();
		testMethod(null, builder::name);
	}
	
	
	@Builder
	@Data
	public static class Person {
		private String name;
		
	}
	
	private static <S, C> void testMethod(Supplier<S> supplier, Consumer<C> consumer) {
		
	}
}
