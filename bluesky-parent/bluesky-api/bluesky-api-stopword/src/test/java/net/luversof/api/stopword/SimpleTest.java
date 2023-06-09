package net.luversof.api.stopword;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import io.lettuce.core.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
class SimpleTest implements GeneralTest {
	
	@Autowired
	private RedisTemplate redisTemplate;
	
//	@Autowired
//	@Qualifier("redisTemplate")
	@Resource(name = "redisTemplate")
	private SetOperations<String, String> setOperations;
	
	@Test
	void test() {
		log.debug("TEST: ");
	}
	
	@Test
	void redisTemplate() {
		assertThat(redisTemplate).isNotNull();
	}
	
	@Test
	void setOperations() {
		assertThat(setOperations).isNotNull();
	}
	
	@Test
	void add() {
		var ret = setOperations.add("stopWord", "금지");
		
		
		log.debug("ret : {}", ret);	// 추가되면 1, 이미 있는 값이면 0
	}
	
	
	@Test
	void check() {
		var ret = setOperations.isMember("stopWord", getStringList().toArray());
//		var ret = setOperations.union("stopWord", getStringList());
		log.debug("ret : {}", ret);
	}
	
	Set<String> getStringList() {
		String name = "금지된캐릭터명입니다";
		var nameSet = new HashSet<String>();
		var length = name.length();
		for (int i = 0; i < length;i++) {
			for (int j = i + 1 ; j <= length; j++) {
				log.debug("length : {}", name.substring(i, j));
				nameSet.add(name.substring(i, j));
			}
		}
		log.debug("set size : {}", nameSet.size());
		log.debug("result : {}", nameSet);
		return nameSet;
	}
}
