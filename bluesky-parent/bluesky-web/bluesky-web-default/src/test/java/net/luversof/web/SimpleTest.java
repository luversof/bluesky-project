package net.luversof.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleTest {

	@Test
	@SneakyThrows
	public void test() {
		ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
		nashorn.eval(read("META-INF/resources/webjars/mustache/2.2.1/mustache.js"));
		nashorn.eval("print('Hello World!');");
		nashorn.eval("var rend = Mustache.render");
		// 방법0. 기본 호출
		//nashorn.eval("print(Mustache.render('test : {{id}}', { 'id' : 'ㅋㅋㅋㅋㅋㅋ'}));");
		nashorn.eval("print(Mustache.render('test : {{id}}, :: {{test}}', { \"id\" : \"ㅋㅋㅋㅋㅋㅋ\"}));");
		
		
		// 방법0-2 lambda까지 호출하기
		nashorn.eval("var data = { \"id\" : \"ㅌㅌㅌㅌㅌㅌ\"}");
		nashorn.eval("data.test = function() {return 'zzzz'}");
		nashorn.eval("var result = Mustache.render('test : {{id}}, :: {{test}}', data);");
		
		log.debug("result : {}", nashorn.get("result"));
		
		//방법1. Java Map을 매개변수로 넘기는 방법
		Invocable invocable = (Invocable) nashorn;
		Map<Object, Object> map = new HashMap<>();
		map.put("id",  "testatse");
		Object result1 = invocable.invokeFunction("rend", "test : {{id}}, :: {{test}}", map);
		log.debug("result1 : {}", result1);
		
		
		//방법2. Json String -> Java Map 으로 호출하여 넘기기
		ObjectMapper objectMapper = new ObjectMapper();
		Map<?, ?> bb = objectMapper.readValue("{ \"id\" : \"ㅋㅋㅋㅋㅋㅋ\"}", Map.class);
		Object result2 = invocable.invokeFunction("rend", "test : {{id}}, :: {{test}}", bb);
		log.debug("result2 : {}", result2);
	}
	
	@Test
	public void test2() throws IOException {
		XmlMapper xmlMapper = new XmlMapper();
		ClassPathResource classPathResource = new ClassPathResource("test.xml");
		InputStream inputStream = classPathResource.getInputStream();
//		Object a = xmlMapper.readValue(inputStream, MenuList.class);
		
		String urlKey = "url";
		@SuppressWarnings("unchecked")
		List<Map<String, String>> menuList = xmlMapper.readValue(inputStream, List.class);
		log.debug("result1 : {}", menuList);
		
		
		String urlPath = "/a/b/c";
		
		List<Map<String, String>> filterdMenuList1 = menuList.stream()
				.filter(menu -> urlPath.startsWith(menu.get(urlKey)))
				.sorted((menu1, menu2) -> Integer.compare(menu1.get(urlKey).length(), menu2.get(urlKey).length()))
				.collect(Collectors.toList());
		log.debug("result2 : {}", filterdMenuList1);
		
		Object filterdMenuList2 = menuList.stream()
				.filter(menu -> urlPath.startsWith(menu.get(urlKey)))
				.sorted((menu1, menu2) -> Integer.compare(menu1.get(urlKey).length(), menu2.get(urlKey).length()))
				.collect(HashMap::new, Map::putAll, Map::putAll);
		log.debug("result2 : {}", filterdMenuList2);
		
		
		Object a = filterdMenuList1.stream().collect(HashMap::new, Map::putAll, Map::putAll);
		log.debug("result3 : {}", a);
				
		
		
		
		
	}
	
	

	private Reader read(String path) {
		return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(path));
	}
	
	@Autowired
	private RestTemplate restTemplate = new RestTemplate();
	
	@Test
	public void test3() throws URISyntaxException {
		//URI uri = new URI("sts://daoum.net");
		URI uri = URI.create("sts://daoum.net");
		log.debug("protocol : {}", uri.getScheme());
		Object forObject = restTemplate.getForObject(uri, Object.class);
		
		log.debug("forObject : {}", forObject);
	}
	
	
	@Test
	public void test4() {
		
		int[][] arr = Arrays.array(
				new int[]{ 9, 20, 28, 18, 11 }, 
				new int[]{ 30, 1, 21, 17, 28 });
		
		for (int i = 0 ; i < arr[0].length ; i++ ) {
			System.out.println(Integer.toBinaryString(arr[0][i] | arr[1][i]).replace("1", "#").replace("0", " "));
		}
		
	}
}
