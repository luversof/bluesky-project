package net.luversof.web.util;

import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;

@DevCheckUtil
public class TestUtil {

	@DevCheckDescription("test method")
	public static String testMethod() {
		return "Test";
	}
	
	@DevCheckDescription("test method2")
	public static String testMethod2() {
		return "Test";
	}
}
