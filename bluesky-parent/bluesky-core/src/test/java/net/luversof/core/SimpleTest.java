package net.luversof.core;

import java.io.File;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.stream.Stream;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.salt.StringFixedSaltGenerator;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.exception.CoreErrorCode;

@Slf4j
public class SimpleTest {

	
	@Test
	public void test() {
		System.out.println(MessageFormat.format("test : {0}", "ASDfdsf"));
	}
	
	@Test
	public void pathRegexTest() {
		String path = "^/battleNet/d3/profile/[\\d]*$";
		String testString = "/battleNet/d3/profile/123";
		System.out.println(testString.matches(path)); 
		
	}
	
	@Test
	public void test3() {
		System.out.println("file:///" + System.getProperty("user.home").replaceAll("\\\\", "/") + "/keystore.p12");
	}
	
	
	@Test
	public void homeDirectoryTest() {
		System.out.println(System.getProperty("user.home").replaceAll("\\\\", "/"));
		System.out.println(File.separator);
	}
	
	@Test
	public void test4() {
		CoreErrorCode notExistUser = CoreErrorCode.NOT_EXIST_USER;
		String join = String.join(".",  notExistUser.getClass().getSimpleName(), notExistUser.name());
		log.debug("join : {}", join);
	}
	
	@Test
	public void test5() {
		PathMatcher pathMatcher = new AntPathMatcher();
		//log.debug("result : {}", pathMatcher.match("/board/{test}/list", "/board/tedfdf/list/test"));
		log.debug("result : {}", pathMatcher.match("/board/{a}/article/{a}/**", "/board/tedfdf/article/123"));
		log.debug("result : {}", pathMatcher.match("/board/{test}/article/{articleId}/**", "/board/tedfdf/article"));
		Comparator<String> patternComparator = pathMatcher.getPatternComparator("/board/{test}/article/{articleId}/**");
		
		
		log.debug("result : {}", patternComparator);
	}
	
	@Test
	public void test6() {
		String uncapitalize = StringUtils.uncapitalize("TestTranslate_TET");
		log.debug("translate : {}", uncapitalize);
	}
	
	@Test
	public void test7() {
		Stream.of(1, 2, 3, 4, 5, 6).takeWhile(i -> i <= 3).forEach(System.out::println);
	}
	
	@Test
	public void test8() {
		StandardPBEStringEncryptor encryptor = getStringEncryptor();
		
		String str = "4B01EE59-10FC-11E9-80EB-001DD8B71EF6";
		String encStr = encryptor.encrypt(str);
		log.debug("test : {}, {}", str, encStr);
		String decStr = encryptor.decrypt(encStr);
		log.debug("test : {}", decStr);
//		log.debug("test : {}", encryptor.decrypt("H9hV5rD8yF9glY8uP7aZ/6Jo0suOmLxW+WGeeIt74PlhBnCJqCtgdQ=="));
	}
	
	private StandardPBEStringEncryptor getStringEncryptor() {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setAlgorithm("PBEWithMD5AndDES");
		encryptor.setPassword("test");
		encryptor.setSaltGenerator(new StringFixedSaltGenerator("12345678"));
		
	    return encryptor;
	}
}
