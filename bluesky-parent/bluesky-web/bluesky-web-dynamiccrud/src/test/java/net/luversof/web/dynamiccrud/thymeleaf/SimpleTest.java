package net.luversof.web.dynamiccrud.thymeleaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
import org.springframework.web.util.pattern.PathPatternParser;

public class SimpleTest {

	@Test
	void linkTest() {
//		 Thymeleaf 관련 검색
//		 이것도 쓰기 애매
//		AssignationUtils.parseAssignationSequence(null, url, false);
//		이걸 막아놨네...
//		LiteralSubstitutionUtil.performLiteralSubstitution(input);
		
		String url = "/{projectId}/setting/fragment/{mainMenuId}/{subMenuId}/modalForm/{modalMode:create|update}";
		// 우선 modalMode의 조건문을 제겋나다.
		
		var map = new HashMap<String, String>();
		map.put("projectId", "섬프로젝트");
		map.put("mainMenuId", "메인메뉴");
		map.put("modalMode", "크리크리");
		
		Pattern pattern = Pattern.compile("(\\{)([\\w\\d]*)([\\:\\w\\|]*)(})");
		Matcher matcher = pattern.matcher(url);
		

		
		var replaceUrl = url;
		while (matcher.find()) {
			String key = matcher.group(2);
			if (map.containsKey(key)) {
				replaceUrl = replaceUrl.replace(matcher.group(), map.get(key));
			}
		}
		System.out.println(replaceUrl);
	}
	
	@Test
	void pathPatternTest() {
		var pathPatternParser = new PathPatternParser();
		PathPattern pathPattern = pathPatternParser.parse("/{projectId}/setting/{mainMenuId}/{subMenuId}");
		PathContainer path = PathContainer.parsePath( "/eventAdmin/setting/menu/dbQuery");
		PathMatchInfo matchAndExtract = pathPattern.matchAndExtract(path);
		System.out.println("test : " + matchAndExtract);
	}
	
	@Test
	void urlMatchTest() {
		Pattern pattern = Pattern.compile("(?:\\/[\\w\\d]+)(\\/setting\\/).*");
		String url = "/test/setting/projectA/mainMenuA";
		String url2 = "/test/use/projectA/mainMenuA";
		
		Matcher matcher = pattern.matcher(url);
		System.out.println(matcher.matches());
		
		Matcher matcher2 = pattern.matcher(url2);
		System.out.println(matcher2.matches());
		
	}
	
	@Test
	void listShuppleTest() {
		Random random = new Random();

		var list = List.of("a", "b", "c");
		
		System.out.println(list.get(random.nextInt(list.size())));
	}
	
}
