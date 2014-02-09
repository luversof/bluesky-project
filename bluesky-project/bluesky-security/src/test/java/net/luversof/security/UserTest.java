package net.luversof.security;

import net.luversof.user.UserConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { UserConfig.class, /* MvcConfig.class */}, loader = AnnotationConfigContextLoader.class)
public class UserTest {

//	@Autowired
//	private UserService userService;
	@Test
	public void 테스트() {
//		User user = userService.findByUsername("bluesky");
//		log.debug("user : {}", user);
		log.debug("Test");
	}
}
