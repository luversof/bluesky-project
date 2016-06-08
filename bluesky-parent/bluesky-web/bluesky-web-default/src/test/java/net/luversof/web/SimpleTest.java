package net.luversof.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

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

	private Reader read(String path) {
		return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(path));
	}
}
