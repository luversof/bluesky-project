package net.luversof.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.salt.StringFixedSaltGenerator;
import org.junit.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StopWatch;
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
		PooledPBEStringEncryptor encryptor = getMd5Encryptor("lms_1");
		
		String str = "4B01EE59-10FC-11E9-80EB-001DD8B71EF6";
		String encStr = encryptor.encrypt(str);
		String decStr = encryptor.decrypt(encStr);
		log.debug("test : {}, {}, {}", str, encStr, decStr);
		
	}
	
	@Test
	public void test9() {
		final String password = "I AM SHERLOCKED"; 
		final String salt = KeyGenerators.string().generateKey(); 
		TextEncryptor encryptor = Encryptors.text(password, salt); 
		System.out.println("Salt: \"" + salt + "\"");
		String str = "\"4B01EE59-10FC-11E9-80EB-001DD8B71EF6\"";
		String encStr = encryptor.encrypt("4B01EE59-10FC-11E9-80EB-001DD8B71EF6");
		String decStr = encryptor.decrypt(encStr);
		log.debug("test : {}, {}, {}", encryptor.encrypt("4B01EE59-10FC-11E9-80EB-001DD8B71EF6"));
	}
	
	@Test
	public void test11() throws UnsupportedEncodingException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start("encryptor create");
		
		int forLength = 10;
		
//		Map<Integer, StringEncryptor> encryptorMap = new HashMap<>();
		Map<Integer, PooledPBEStringEncryptor> encryptorMap = new HashMap<>();
		
		// encryptor 생성
		
		IntStream.range(1, forLength + 1).forEach(idx -> {
			encryptorMap.put(idx, getAES128Encryptor("lms_" + idx));
		});
		stopWatch.stop();
		
		String characterId = "D94E0757-2F3D-11E9-80F1-001DD8B71EBC";
		
		// 단일 charId 여러번 암호화 확인 
		IntStream.range(1, forLength + 1).forEach(idx -> {
			String encCharacterId = encryptorMap.get(2).encrypt(characterId);
			String decCharacterId = encryptorMap.get(2).decrypt(encCharacterId);
			log.debug("characterId : {}, enc : {}, dec : {}", characterId, encCharacterId, decCharacterId);
		});
		
		// 서버별 암호화 확인 및 다른 서버키로 복호화 확인
		IntStream.range(1, forLength + 1).forEach(idx -> {
			stopWatch.start("encrypt test");
			String encCharacterId = encryptorMap.get(idx).encrypt(characterId);
			stopWatch.stop();
			stopWatch.start("decrypt test");
			String decCharacterId = encryptorMap.get(idx).decrypt(encCharacterId);
			stopWatch.stop();
			assertThat(characterId.equals(decCharacterId));
			log.debug("characterId : {}, enc : {}, dec : {}", characterId, encCharacterId, decCharacterId);
			
			IntStream.range(1, forLength + 1).forEach(idx2 -> {
				if (idx != idx2) {
					assertThatExceptionOfType(EncryptionOperationNotPossibleException.class).isThrownBy(() -> {
						String decCharacterId2 = encryptorMap.get(idx2).decrypt(decCharacterId);

						if (decCharacterId2.length() != 36) {
							throw new EncryptionOperationNotPossibleException();
						}
					});
				}
				
			});
			
		});
		
		System.out.println(stopWatch.prettyPrint());
		
	}
	
	private PooledPBEStringEncryptor getMd5Encryptor(String password) {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		encryptor.setPassword(password);
		encryptor.setAlgorithm("PBEWithMD5AndDES");
		encryptor.setPoolSize(1);
		encryptor.setSaltGenerator(new StringFixedSaltGenerator("1234567890123456"));
		return encryptor;
	}
	
	private PooledPBEStringEncryptor getAES128Encryptor(String password) {
		Security.addProvider(new BouncyCastleProvider());
		
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		encryptor.setPassword(password);
		encryptor.setProviderName("BC");
		encryptor.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
		encryptor.setPoolSize(1);
		encryptor.setSaltGenerator(new StringFixedSaltGenerator("1234567890123456"));
		return encryptor;
	}
}
