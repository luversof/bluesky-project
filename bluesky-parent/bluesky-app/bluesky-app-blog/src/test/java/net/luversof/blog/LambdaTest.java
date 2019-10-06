package net.luversof.blog;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class LambdaTest extends GeneralTest {
	
	@Test
	public void test() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		LambdaTestService a = new LambdaTestService();
		Method method = LambdaTestService.class.getMethod("getTest");
		
	
		
		
		log.debug("test : {}", method.toGenericString());
		log.debug("test : {}", method.invoke(a));
		
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test3() throws FileNotFoundException, ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("C:\\Users\\luver\\git\\bluesky-project\\bluesky-parent\\bluesky-app\\bluesky-app-blog\\target\\test-classes\\script.js"));
		ScriptEngine engine2 = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("C:\\Users\\luver\\git\\bluesky-project\\bluesky-parent\\bluesky-app\\bluesky-app-blog\\target\\test-classes\\script.js"));
		
		log.debug("Test : ");
	}
}
