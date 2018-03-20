package net.luversof.opensource.cloud.stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Processor;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class ProcessorTest extends GeneralTest {
	
	@Autowired
	private Processor processor;

	@Test
	public void test() {
		log.debug("test : {}", processor);
	}
}
