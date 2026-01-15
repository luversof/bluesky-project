package net.luversof;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class GeneralTestImplTest implements GeneralTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testContextLoads() {
		assertThat(applicationContext).isNotNull();
	}

}
