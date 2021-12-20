package net.luversof.web.util;

import io.github.luversof.boot.autoconfigure.devcheck.core.annotation.DevCheckDescription;

public class TestUtilExtends extends TestUtil {

	@DevCheckDescription("test method")
	public static String testMethodExtends() {
		return "testMethod3";
	}
}
