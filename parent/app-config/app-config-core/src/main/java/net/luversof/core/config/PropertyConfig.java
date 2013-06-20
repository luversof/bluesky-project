package net.luversof.core.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.spring31.properties.EncryptablePropertyPlaceholderConfigurer;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * properties를 암호화 하기 위한 설정
 * todo : systemProperties를 &#064;Configuration에서 읽어 들이는 방법을 알아야 함.
 * @author bluesky
 *
 */
@Configuration
public class PropertyConfig {

	@Bean
	public BouncyCastleProvider bouncyCastleProvider() {
		return new BouncyCastleProvider();
	}

	@Bean
	public EnvironmentPBEConfig environmentStringPBEConfig() {
		EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
		environmentStringPBEConfig.setProvider(bouncyCastleProvider());
		environmentStringPBEConfig.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
		environmentStringPBEConfig.setPassword("bluesky");
		return environmentStringPBEConfig;
	}

	@Bean
	public StrongPasswordEncryptor strongPasswordEncryptor() {
		return new StrongPasswordEncryptor();
	}

	@Bean
	public StandardPBEStringEncryptor standardPBEStringEncryptor() {
		StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
		standardPBEStringEncryptor.setConfig(environmentStringPBEConfig());
		return standardPBEStringEncryptor;
	}

	/**
	 * properties 정보 암호화 처리
	 * @param springProfilesActive
	 * @return
	 */
	@Bean
	public EncryptablePropertyPlaceholderConfigurer encryptablePropertyPlaceholderConfigurer(@Value("#{systemProperties['spring.profiles.active']}") String springProfilesActive) {
		EncryptablePropertyPlaceholderConfigurer encryptablePropertyPlaceholderConfigurer = new EncryptablePropertyPlaceholderConfigurer(standardPBEStringEncryptor());

		Resource[] locations = {
			new ClassPathResource("net/luversof/core/config/property/repository-" + springProfilesActive + ".properties"),	
		};
		encryptablePropertyPlaceholderConfigurer.setLocations(locations);
		return encryptablePropertyPlaceholderConfigurer;
	}

}
