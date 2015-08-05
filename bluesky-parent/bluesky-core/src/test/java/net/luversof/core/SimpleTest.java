package net.luversof.core;

import java.text.MessageFormat;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class SimpleTest {

	@Test
	public void encryptTest() {
		log.debug(EncryptionUtil.stringEncryptor().encrypt("cntvyqjqspexfebzd42qte5faa4jgyaz"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("hXFh2Bex4mKhHBqtEMEGavYntvGVzRuP"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/authorize"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/token"));
		log.debug(EncryptionUtil.stringEncryptor().encrypt("https://kr.battle.net/oauth/check_token?token={accessToken}"));
	}
	
	@Test
	public void decryptTest() {
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("YqDM2O1+id5k47FhbLS9A5B8mzjGbNgYFYM3kw47Mxo="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("6DPA7fbFEmJNFv6us3sjBy5d2HSsTLXQ8EynzRIC4VBrvUEKsm590uLu/APjQ5okTFdtgYpf5stFNC/5fhJqsw=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("3rsnOk5Ox0uDDFu8F8OhzqoajL7xcRlz+ANBUYnntbR1ovTZKVRzHe47Eqo/f1M2BJf/5P/dwG5EYwC6yPQBhA=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("+/NGSFG8dWC9QTD2i7PPy8fW37idOkwpoV6fsvX7bNPrkMp/tUViFqvoXvrbGlLrHkoPyRVBRwjFdJv6fs74Sw=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("wwHWjUOSgp/HVr++b/M0m9kEGePLdgsgAi648yQPAfTr2hjqCZc6pSZ/1OT9/O1PIBz1zwk2B4g9woCTGERf9W6necbkTxGone22WcBgBk0="));
	}
	
	@Test
	public void test() {
		System.out.println(MessageFormat.format("test : {0}", "ASDfdsf"));
	}
}
