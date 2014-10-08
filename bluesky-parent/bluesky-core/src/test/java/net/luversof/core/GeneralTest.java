package net.luversof.core;

import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ContextConfiguration(classes = { BlueskyCoreConfig.class, GeneralTest.class }, loader = AnnotationConfigContextLoader.class, initializers = BlueskyApplicationContextInitializer.class)
@PropertySource(name = "jdbcProp", value = "classpath:test.properties")
public abstract class GeneralTest {

}