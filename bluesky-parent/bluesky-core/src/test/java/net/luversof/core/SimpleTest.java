package net.luversof.core;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class SimpleTest {

	@Test
	public void test() {
		log.debug(EncryptionUtil.stringEncryptor().encrypt("bluesky"));
	}
	
	@Test
	public void decryptTest() {
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("7pGeulnHcAWNUfpR8ne4SsC+zMzj4NZhxMOprRrnUXE="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("EmyiujHedqYmdaSJxCHYTFqtgA7emseOssZRQ1qCHO8="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("1pCmym1zX1XVwzWftN8y2k1R2MgEZM2jGmM2cpdR+x8="));
	}
}
