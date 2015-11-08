package net.luversof.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class EncryptionUtil {

	private static StandardPBEStringEncryptor STRING_ENCRYPTOR = new StandardPBEStringEncryptor();
	
	static {
		EnvironmentPBEConfig PBE_CONFIG = new EnvironmentPBEConfig();
		PBE_CONFIG.setProvider(new BouncyCastleProvider());
		PBE_CONFIG.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
		PBE_CONFIG.setPassword("bluesky");
		STRING_ENCRYPTOR.setConfig(PBE_CONFIG);
	}
	
	public static StringEncryptor stringEncryptor() {
		return STRING_ENCRYPTOR;
	}
}
