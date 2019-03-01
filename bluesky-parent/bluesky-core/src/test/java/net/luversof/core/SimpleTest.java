package net.luversof.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Security;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEBigIntegerEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.PBEConfig;
import org.jasypt.encryption.pbe.config.SimplePBEConfig;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.registry.AlgorithmRegistry;
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
		log.debug("result : {}", getAlphaNumericString(19));
	}
	
	@Test
	public void test8() {
		log.debug("test : {}", KeyGenerators.string().generateKey());
	}
	
	@Test
	public void textEncryptorTest() {
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
	public void getAllAlgorithms() {
		log.debug("allDigestAlgorithms : {}", AlgorithmRegistry.getAllDigestAlgorithms());
		log.debug("allPBEAlgorithms : {}", AlgorithmRegistry.getAllPBEAlgorithms());
	}
	
	@Test
	public void encryptSimpleTest() {
		StringEncryptor md5Encryptor = getEncryptor("somePassword");
		String str = "testString";
		String encStr = md5Encryptor.encrypt(str);
		String decStr = md5Encryptor.decrypt(encStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, encStr, decStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, md5Encryptor.encrypt(str), decStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, md5Encryptor.encrypt(str), decStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, md5Encryptor.encrypt(str), decStr);
		log.debug("str : {}, encStr : {}, decStr : {}", str, md5Encryptor.encrypt(str), decStr);
	}
	
	@Test
	public void encryptTest() throws UnsupportedEncodingException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start("encryptor create");
		
		int encryptTestCount = 100;
		int testStrCount = 10;
		
		Map<Integer, StringEncryptor> encryptorMap = new HashMap<>();
		
		// encryptor 생성
		
		IntStream.range(1, encryptTestCount + 1).forEach(idx -> {
			encryptorMap.put(idx, getEncryptor("somePassword_" + idx));
		});
		stopWatch.stop();
		
		stopWatch.start("단일 호출 test");
		IntStream.range(1,  testStrCount + 1).forEach(idx -> {
			singleEncryptorTest(encryptorMap, 2, encryptTestCount, UUID.randomUUID().toString().toUpperCase());
		});
		stopWatch.stop();
		
		stopWatch.start("암복호화 test");
		IntStream.range(1,  testStrCount + 1).forEach(idx -> {
			multiEncryptorTest(encryptorMap, encryptTestCount, UUID.randomUUID().toString().toUpperCase());
		});
		stopWatch.stop();
		
		System.out.println(stopWatch.prettyPrint());
		log.debug("total Second : {}", stopWatch.getTotalTimeSeconds());
		
	}
	
	// 단일 encryptor 여러번 호출
	private void singleEncryptorTest(Map<Integer, StringEncryptor> encryptorMap, int mapKey, int encryptTestCount, String str) {
		IntStream.range(1, encryptTestCount + 1).forEach(idx -> {
			String encStr = encryptorMap.get(mapKey).encrypt(str);
			String decStr = encryptorMap.get(mapKey).decrypt(encStr);
			if (!str.equals(decStr)) {
				log.debug("str : {}, encStr : {}, decStr : {}", str, encStr, decStr);
			}
		});
	}
	
	// 여러개 encryptor 암복호화 확인
	private void multiEncryptorTest(Map<Integer, StringEncryptor> encryptorMap, int encryptTestCount, String str) {
		// 서버별 암호화 확인 및 다른 서버키로 복호화 확인
		IntStream.range(1, encryptTestCount + 1).forEach(idx -> {
			
			String encStr = encryptorMap.get(idx).encrypt(str);
			String decStr = encryptorMap.get(idx).decrypt(encStr);
			assertThat(str.equals(decStr));
				
			if (!str.equals(decStr)) {
				log.debug("str : {}, encStr : {}, decStr : {}", str, encStr, decStr);
			}
			
			IntStream.range(1, encryptTestCount + 1).forEach(idx2 -> {
				if (idx != idx2) {
					assertThatExceptionOfType(EncryptionOperationNotPossibleException.class).isThrownBy(() -> {
						if (encryptorMap.get(idx2).decrypt(decStr).length() != 36) {
							throw new EncryptionOperationNotPossibleException();
						}
					});
				}
				
			});
			
		});
	}
	
	private StringEncryptor getEncryptor(String password) {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		encryptor.setConfig(getPBEConfig(password));
		return encryptor;
	}
	
	private PBEConfig getPBEConfig(String password) {
		SimplePBEConfig config = new SimplePBEConfig();
		config.setAlgorithm("PBEWithMD5AndDES");
		config.setProvider(new BouncyCastleProvider());
		//config.setAlgorithm("PBEWithSHA256And128BitAES-CBC-BC");
		config.setPassword(password);
		config.setPoolSize(2);
		config.setSaltGenerator(new StringFixedSaltGenerator(getAlphaNumericString(16)));
		return config;
	}
	
	private String getAlphaNumericString(int n) { 
        // chose a Character random from this String 
        // String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789";
   		String AlphaNumericString = "~!@#$%^&*()_+|";
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
			int index = (int) (AlphaNumericString.length() * Math.random()); 
  
            // add Character one by one in end of sb 
			sb.append(AlphaNumericString.charAt(index)); 
        }
        return sb.toString(); 
    } 
  
	
	@Test
	public void checkSupportAlgorithm() {
		Security.addProvider(new BouncyCastleProvider());
		List<Object> supported = new ArrayList<>();
		List<Object> unsupported = new ArrayList<>();
		for (Object algorithm : AlgorithmRegistry.getAllPBEAlgorithms()) {
			try {
				StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
				encryptor.setPassword("somePassword");
				encryptor.setAlgorithm(String.valueOf(algorithm));

				String str = "test";
				String encStr = encryptor.encrypt(str);
				String decStr = encryptor.decrypt(encStr);
				assertThat(str.equals(decStr));
				supported.add(algorithm);
			} catch (EncryptionOperationNotPossibleException e) {
				unsupported.add(algorithm);
			}
		}
		log.debug("supported : {}", supported);
		log.debug("unsupported : {}", unsupported);
	}
	
	@Test
	public void bigIntegerEncryptorTest() {
		  StandardPBEBigIntegerEncryptor encryptor = new StandardPBEBigIntegerEncryptor();
		  encryptor.setAlgorithm("PBEWithMD5AndDES");
		  encryptor.setPassword("somePassword");
		  log.debug("test : {}", encryptor.encrypt(new BigInteger("123123")));
	}
	
}
