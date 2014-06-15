package net.luversof.core;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class SimpleTest {

	@Test
	public void test() {
		log.debug(EncryptionUtil.stringEncryptor().encrypt("test"));
	}
}
