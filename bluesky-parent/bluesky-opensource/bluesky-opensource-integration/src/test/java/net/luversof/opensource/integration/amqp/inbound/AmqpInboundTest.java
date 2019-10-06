package net.luversof.opensource.integration.amqp.inbound;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class AmqpInboundTest extends GeneralTest {

	@Test
	public void test() throws InterruptedException {
		log.debug("inboundTest..");
		TimeUnit.SECONDS.sleep(10);
	}
}
