package net.luversof.core;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { BlueskyCoreConfig.class }, loader = AnnotationConfigContextLoader.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {

}