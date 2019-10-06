package net.luversof.opensource.integration.amqp.outbound;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.opensource.integration.amqp.outbound.AmqpOutboundTestConfig.MyGateway;

@Slf4j
public class AmqpOutboundTest extends GeneralTest {
	
	@Autowired
	private MyGateway gateway;

	@Test
	public void test() {
		for(int i = 0 ; i < 500; i++) {
			gateway.sendToRabbit("테스트 코드를 발송해보자." + i);
		}
		
		log.debug("end");
	}
}
