//package net.luversof.opensource.cloud.stream;
//
//import org.springframework.cloud.stream.annotation.EnableBinding;
//import org.springframework.cloud.stream.annotation.StreamListener;
//import org.springframework.cloud.stream.messaging.Sink;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@EnableBinding(Sink.class)
//public class LoggingConsumerConfiguration {
//
//	@StreamListener(Sink.INPUT)
//	public void handle(Person person) {
//		System.out.println("Received: " + person);
//	}
//
//	public static class Person {
//		private String name;
//		public String getName() {
//			return name;
//		}
//		public void setName(String name) {
//			this.name = name;
//		}
//		public String toString() {
//			return this.name;
//		}
//	}
//}