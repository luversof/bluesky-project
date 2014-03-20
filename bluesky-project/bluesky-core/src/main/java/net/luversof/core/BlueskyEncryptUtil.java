package net.luversof.core;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;
import org.jasypt.encryption.pbe.config.PBEConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class BlueskyEncryptUtil {

	public static PBEConfig pBEConfig() {
		EnvironmentPBEConfig pBEConfig = new EnvironmentPBEConfig();
		pBEConfig.setProvider(new BouncyCastleProvider());
		pBEConfig.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
		pBEConfig.setPassword("bluesky");
		return pBEConfig;
	}
	
	public static StringEncryptor stringEncryptor() {
		StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
		standardPBEStringEncryptor.setConfig(pBEConfig());
		return standardPBEStringEncryptor;
		
	}
}
