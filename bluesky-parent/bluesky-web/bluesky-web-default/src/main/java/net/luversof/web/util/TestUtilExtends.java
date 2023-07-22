package net.luversof.web.util;

import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;

public class TestUtilExtends extends TestUtil {

	@DevCheckDescription("test method")
	public static String testMethodExtends() {
		return "testMethod3";
	}
}
