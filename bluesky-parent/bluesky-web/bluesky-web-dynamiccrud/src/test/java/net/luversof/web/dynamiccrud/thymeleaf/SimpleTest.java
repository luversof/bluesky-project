package net.luversof.web.dynamiccrud.thymeleaf;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

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
}
