package net.luversof.web;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.opensource.jdbc.routing.DataSourceContextHolder;
import net.luversof.opensource.jdbc.routing.DataSourceType;
import net.luversof.user.repository.UserRepository;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class TestClass extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Test
	@Transactional
	public void test(){
		DataSourceContextHolder.setDataSourceType(DataSourceType.MEMBER);
		log.debug("datasource : {}", DataSourceContextHolder.getDataSourceType());
		userRepository.findByUsername("bluesky");
		
		log.debug("user : {}", userRepository.findByUsername("bluesky"));
	}
}
