//package net.luversof.opensource.cloud.stream;
//
//import org.junit.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.stream.annotation.EnableBinding;
//import org.springframework.cloud.stream.messaging.Processor;
//import org.springframework.cloud.stream.test.binder.MessageCollector;
//import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
//import org.springframework.integration.annotation.Transformer;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.GenericMessage;
//
//import lombok.extern.slf4j.Slf4j;
//import net.luversof.GeneralTest;
//
//@Slf4j
//public class TestSupportTest extends GeneralTest {
//
//	@Autowired
//	private Processor processor;
//
//	@Autowired
//	private MessageCollector messageCollector;
//
//	@Test
//	public void test() {
//		log.debug("test : {}", messageCollector);
//	}
//
//	@Test
//	public void testWiring() {
//		Message<String> message = new GenericMessage<>("hello");
//		processor.input().send(message);
////		Message<String> received = (Message<String>) messageCollector.forChannel(processor.output()).poll();
////		log.debug("test : {}", received.getPayload());
//	}
//	
//	@Test
//	public void test2() {
//		Message<String> received = (Message<String>) messageCollector.forChannel(processor.output()).poll();
//		log.debug("test : {}", received.getPayload());
//	}
//
//	@SpringBootApplication(exclude = TestSupportBinderAutoConfiguration.class)
//	@EnableBinding(Processor.class)
//	public static class MyProcessor {
//
//		@Autowired
//		private Processor channels;
//
//		@Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
//		public String transform(String in) {
//			return in + " world";
//		}
//	}
//}
