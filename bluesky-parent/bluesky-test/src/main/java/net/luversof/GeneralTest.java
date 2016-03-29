package net.luversof;

import net.luversof.TestApplication;
import net.luversof.core.context.BlueskyApplicationContextInitializer;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class, initializers = BlueskyApplicationContextInitializer.class)
public abstract class GeneralTest {
	public String getRemoteAddr() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "127.0.0.1";
		}
	}
}
