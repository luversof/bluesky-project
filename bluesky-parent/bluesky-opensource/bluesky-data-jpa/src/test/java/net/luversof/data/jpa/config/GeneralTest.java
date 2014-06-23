package net.luversof.data.jpa.config;

import net.luversof.core.BlueskyApplicationContextInitializer;
import net.luversof.data.jpa.JpaConfig;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JpaConfig.class }, loader = AnnotationConfigContextLoader.class, initializers = BlueskyApplicationContextInitializer.class)
// @IfProfileValue(name="spring.profiles.active", value="dev")
// @ActiveProfiles("dev")
public abstract class GeneralTest {

}