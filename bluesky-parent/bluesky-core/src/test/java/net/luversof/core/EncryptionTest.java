package net.luversof.core;

import net.luversof.GeneralTest;
import net.luversof.TestConfig.TestClass;

import java.text.MessageFormat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncryptionTest extends GeneralTest {

	@Value("${test.value}")
	String testValue;
	
	@Value("${test.value2}")
	String testValue2;
	
	@Value("${test.value3}")
	String testValue3;
	
	@Test
	public void test() {
		log.debug("result : {}, {}, {}", testValue, testValue2, testValue3);
		
		
		log.debug("result2 : {}", MessageFormat.format("test {0}", 123));
		
		log.debug("result3 : {}", MessageFormat.format("jdbc:mysql://{0}:{1}/{2}?useSSL=false&useUnicode=true&autoReconnection=true", "127.0.0.1", "3306", "user"));
	}
	
	@Value("${dataSource.blog.serverName}")
	String test;
	
	@Test
	public void test2() {
		log.debug("test : {}", test);
	}
	
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private TestClass testClass;
	
	@Test
	public void test3() {
		
		log.debug("environment.getActiveProfiles() : {}", environment.getActiveProfiles());
		log.debug("testClass : {}", testClass);
	}
	
}
