package net.luversof.core;

import java.text.MessageFormat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

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
	
	@Value("${dataSource.blog.password}")
//	@Value("${spring.config.name}")
	String test;
	
	@Test
	public void test2() {
		log.debug("test : {}", test);
	}
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Test
	public void test3() {
		log.debug("test : {}", applicationContext.getMessage("HttpRequestMethodNotSupportedException", null, null));
	}
}
