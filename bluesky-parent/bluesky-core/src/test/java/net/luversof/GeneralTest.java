package net.luversof;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.luversof.core.context.BlueskyApplicationContextInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestApplication.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {


}
