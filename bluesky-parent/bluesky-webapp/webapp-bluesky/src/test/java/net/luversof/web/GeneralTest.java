package net.luversof.web;

import net.luversof.blog.BlogConfig;
import net.luversof.core.BlueskyApplicationContextInitializer;
import net.luversof.user.UserConfig;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { UserConfig.class, BlogConfig.class }, loader = AnnotationConfigContextLoader.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {

}
