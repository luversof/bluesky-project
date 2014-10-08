package net.luversof.core;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

public class EncryptionTest extends GeneralTest {

	@Value("${dataSource.bookkeeping.user}")
	String testValue;
	
	@Test
	public void test() {
		System.out.println(testValue);
	}
}
