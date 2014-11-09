package net.luversof.core;

import java.text.MessageFormat;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class SimpleTest {

	@Test
	public void encryptTest() {
		log.debug(EncryptionUtil.stringEncryptor().encrypt("5ce0e9ac811fd9c04543"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("b280853a74e6ae138ac23805092ddca670624ac9"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://github.com/login/oauth/authorize"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://github.com/login/oauth/access_token"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://api.github.com/applications/{clientId}/tokens/{accessToken}"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("124755777691522"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("3e543ae36e0185e44b1504ec1ec717c9"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://www.facebook.com/dialog/oauth"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://graph.facebook.com/oauth/access_token"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://graph.facebook.com/debug_token?input_token={inputToken}"));
	}
	
	@Test
	public void decryptTest() {
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("7pGeulnHcAWNUfpR8ne4SsC+zMzj4NZhxMOprRrnUXE="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("EmyiujHedqYmdaSJxCHYTFqtgA7emseOssZRQ1qCHO8="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("1pCmym1zX1XVwzWftN8y2k1R2MgEZM2jGmM2cpdR+x8="));
	}
	
	@Test
	public void test() {
		System.out.println(MessageFormat.format("test : {0}", "ASDfdsf"));
	}
}
