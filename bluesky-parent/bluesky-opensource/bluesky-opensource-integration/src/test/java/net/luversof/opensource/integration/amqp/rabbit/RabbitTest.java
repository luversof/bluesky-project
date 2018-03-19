package net.luversof.opensource.integration.amqp.rabbit;

import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class RabbitTest extends GeneralTest {
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@Test
	public void test() {
		for(int i = 0 ; i < 500; i++) {
			rabbitTemplate.convertAndSend("foo2", "테스트 코드를 발송해보자." + i);
		}
		
		log.debug("end");
	}
}
