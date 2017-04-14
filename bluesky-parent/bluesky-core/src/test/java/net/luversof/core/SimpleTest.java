package net.luversof.core;

import java.io.File;
import java.text.MessageFormat;
import java.util.Comparator;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.exception.ErrorCode;
import net.luversof.core.util.EncryptionUtil;

import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Slf4j
public class SimpleTest {

	@Test
	public void encryptTest() {
		log.debug(EncryptionUtil.stringEncryptor().encrypt("cntvyqjqspexfebzd42qte5faa4jgyaz"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("hXFh2Bex4mKhHBqtEMEGavYntvGVzRuP"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/authorize"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/token"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/check_token?token={accessToken}"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("1"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("2"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("3"));
	}
	
	@Test
	public void decryptTest() {
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("9yCoelU9aywYujEB2jdGtxMkHSVlokiq/3rEiuP0uojOxMFppeqrujO6pkcE1sBFrRnJRcnjCs8P1Eax+/dHSg=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("gsW4zNB9hZzapDH6MZQCrLUlvrfefew+gcFrTeaE1QNbK4fjcUkeld42FP6XiCBlOgIKFhpaTF+H0cY/fMA3nw=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("6NCyrc+JD2ys2F/OGcMFCylRcRZK9DmIbLvJfmn8T2agBMPqRzlou3po7tDQgqt7WGSAVFP7N9/DdreJIS0boKszEhnkKOzKP1k2kIi86y5ZTITP5W31syVkpN9xknUo"));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("3rsnOk5Ox0uDDFu8F8OhzqoajL7xcRlz+ANBUYnntbR1ovTZKVRzHe47Eqo/f1M2BJf/5P/dwG5EYwC6yPQBhA=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("+/NGSFG8dWC9QTD2i7PPy8fW37idOkwpoV6fsvX7bNPrkMp/tUViFqvoXvrbGlLrHkoPyRVBRwjFdJv6fs74Sw=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("wwHWjUOSgp/HVr++b/M0m9kEGePLdgsgAi648yQPAfTr2hjqCZc6pSZ/1OT9/O1PIBz1zwk2B4g9woCTGERf9W6necbkTxGone22WcBgBk0="));
	}
	
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
		ErrorCode notExistUser = ErrorCode.NOT_EXIST_USER;
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
}
