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
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("2g/ceEVbeuNTSgq2bxypz0b6O0daT/mpiW7mX3709y8qzbgLUw99PMy/fBk7WqwPMdQGwnzmK+aBFJttQdtuuA=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("9hHiOAJi9juPVlbfr2AQRiKHwhsZXtD4Xp3RLdyRlaq2uG6PtKaf2TRmvWR65nRL0awcOR1InjPhajM091bDCQ=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("6NCyrc+JD2ys2F/OGcMFCylRcRZK9DmIbLvJfmn8T2agBMPqRzlou3po7tDQgqt7WGSAVFP7N9/DdreJIS0boKszEhnkKOzKP1k2kIi86y5ZTITP5W31syVkpN9xknUo"));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("3rsnOk5Ox0uDDFu8F8OhzqoajL7xcRlz+ANBUYnntbR1ovTZKVRzHe47Eqo/f1M2BJf/5P/dwG5EYwC6yPQBhA=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("+/NGSFG8dWC9QTD2i7PPy8fW37idOkwpoV6fsvX7bNPrkMp/tUViFqvoXvrbGlLrHkoPyRVBRwjFdJv6fs74Sw=="));
		System.out.println(EncryptionUtil.stringEncryptor().decrypt("wwHWjUOSgp/HVr++b/M0m9kEGePLdgsgAi648yQPAfTr2hjqCZc6pSZ/1OT9/O1PIBz1zwk2B4g9woCTGERf9W6necbkTxGone22WcBgBk0="));
	}
	
	@Test
	public void test() {
		System.out.println(MessageFormat.format("test : {0}", "ASDfdsf"));
	}
}
