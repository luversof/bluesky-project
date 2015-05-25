package net.luversof;

import net.luversof.TestApplication;
import net.luversof.core.BlueskyApplicationContextInitializer;

import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {

}
