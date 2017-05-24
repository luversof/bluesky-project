package net.luversof;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.Data;

@Configuration
public class TestConfig {
	
	@Bean
	public TestClass TestClass() {
		return new TestClass("testValue");
	}
	
	@Data
	public static class TestClass {
		private String a;

		public TestClass(String a) {
			this.a = a;
		}
		
	}
	
	
}
