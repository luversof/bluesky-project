package net.luversof.security;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.config.AppConfig;
import net.luversof.security.domain.User;
import net.luversof.security.service.UserService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class /* , MvcConfig.class */}, loader = AnnotationConfigContextLoader.class)
public class UserTest {

	@Autowired
	private UserService userService;
	@Test
	public void 테스트() {
		User user = userService.findOne(1);
		log.debug("user : {}", user);
	}
}
