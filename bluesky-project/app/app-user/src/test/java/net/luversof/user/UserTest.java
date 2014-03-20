package net.luversof.user;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.BlueskyApplicationContextInitializer;
import net.luversof.data.jpa.JpaConfig;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JpaConfig.class, UserConfig.class /* , MvcConfig.class */}, loader = AnnotationConfigContextLoader.class, initializers = BlueskyApplicationContextInitializer.class)
public class UserTest {

	@Autowired
	private UserService userService;
	@Test
	public void 테스트() {
		User user = userService.findByUsername("bluesky");
		log.debug("user : {}", user);
	}
}
