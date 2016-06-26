package net.luversof.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleTest {

	@Test
	public void test() throws Exception {
		ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
		try {
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
			
		
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void test2() throws JsonParseException, JsonMappingException, IOException {
		XmlMapper xmlMapper = new XmlMapper();
		ClassPathResource classPathResource = new ClassPathResource("test.xml");
		InputStream inputStream = classPathResource.getInputStream();
//		Object a = xmlMapper.readValue(inputStream, MenuList.class);
		
		String urlKey = "url";
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
}
