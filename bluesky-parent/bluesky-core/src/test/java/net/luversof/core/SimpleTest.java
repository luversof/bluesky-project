package net.luversof.core;

import java.io.File;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.exception.CoreErrorCode;

@Slf4j
class SimpleTest {

	
	@Test
	void test() {
		System.out.println(MessageFormat.format("test : {0}", "ASDfdsf"));
	}
	
	@Test
	void pathRegexTest() {
		String path = "^/battleNet/d3/profile/[\\d]*$";
		String testString = "/battleNet/d3/profile/123";
		System.out.println(testString.matches(path)); 
		
	}
	
	@Test
	void test3() {
		System.out.println("file:///" + System.getProperty("user.home").replaceAll("\\\\", "/") + "/keystore.p12");
	}
	
	
	@Test
	void homeDirectoryTest() {
		System.out.println(System.getProperty("user.home").replaceAll("\\\\", "/"));
		System.out.println(File.separator);
	}
	
	@Test
	void test4() {
		CoreErrorCode notExistUser = CoreErrorCode.NOT_EXIST_USER;
		String join = String.join(".",  notExistUser.getClass().getSimpleName(), notExistUser.name());
		log.debug("join : {}", join);
	}
	
	@Test
	void test5() {
		PathMatcher pathMatcher = new AntPathMatcher();
		//log.debug("result : {}", pathMatcher.match("/board/{test}/list", "/board/tedfdf/list/test"));
		log.debug("result : {}", pathMatcher.match("/board/{a}/article/{a}/**", "/board/tedfdf/article/123"));
		log.debug("result : {}", pathMatcher.match("/board/{test}/article/{articleId}/**", "/board/tedfdf/article"));
		Comparator<String> patternComparator = pathMatcher.getPatternComparator("/board/{test}/article/{articleId}/**");
		
		
		log.debug("result : {}", patternComparator);
	}
	
	@Test
	void test6() {
		String uncapitalize = StringUtils.uncapitalize("TestTranslate_TET");
		log.debug("translate : {}", uncapitalize);
	}
	
	@Test
	void test8() {
		log.debug("test : {}", KeyGenerators.string().generateKey());
	}
	
	@Test
	void textEncryptorTest() {
		final String password = "somePassword"; 
		final String salt = KeyGenerators.string().generateKey(); 
		TextEncryptor encryptor = Encryptors.text(password, salt); 
		System.out.println("Salt: \"" + salt + "\"");
		String str = "4B01EE59-10FC-11E9-80EB-001DD8B71EF6";
		String encStr = encryptor.encrypt(str);
		String decStr = encryptor.decrypt(encStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, encStr, decStr);
	}
	
	
	
	@Test
	void test9() {
		log.debug("test : {}", TimeUnit.MINUTES.toMillis(10));
	}
}
