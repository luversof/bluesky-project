package net.luversof;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import net.luversof.core.context.BlueskyApplicationContextInitializer;

@RunWith(SpringRunner.class)
//@SpringApplicationConfiguration(classes = TestApplication.class, initializers = BlueskyApplicationContextInitializer.class)
@ContextConfiguration(classes = TestApplication.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {
}
