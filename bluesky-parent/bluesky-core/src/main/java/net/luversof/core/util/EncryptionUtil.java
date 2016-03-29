package net.luversof.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class EncryptionUtil {

	private static StandardPBEStringEncryptor StringEncryptor = new StandardPBEStringEncryptor();
	
	static {
		EnvironmentPBEConfig config = new EnvironmentPBEConfig();
		config.setProvider(new BouncyCastleProvider());
		config.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
		config.setPassword("bluesky");
		StringEncryptor.setConfig(config);
	}
	
	public static StringEncryptor stringEncryptor() {
		return StringEncryptor;
	}
}
