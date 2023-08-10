package net.luversof.api.board;

import org.springframework.lang.Nullable;

import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;
import io.github.luversof.boot.devcheck.annotation.DevCheckUtil;
import net.luversof.api.board.domain.Board;

@DevCheckUtil
public class TestUtil {

	@DevCheckDescription("test method")
	public static String testMethod(Board board) {
		return "Test";
	}
	
	@DevCheckDescription("test method2")
	public static Board testMethod2(String a, int b, Board board) {
		return null;
	}
	
	public static Board testMethod3(String a, int b, Board board) {
		return null;
	}
	
	public static Board testMethod4() {
		return null;
	}
	
	public static Board testMethod4(@Nullable String a) {
		return null;
	}
}
