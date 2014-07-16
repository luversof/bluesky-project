package net.luversof.web;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.jdbc.datasource.DataSourceContextHolder;
import net.luversof.jdbc.datasource.DataSourceType;
import net.luversof.user.repository.UserRepository;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
		DataSourceContextHolder.setDataSourceType(DataSourceType.SECURITY);
		log.debug("datasource : {}", DataSourceContextHolder.getDataSourceType());
		userRepository.findByUsername("bluesky");
		
		log.debug("user : {}", userRepository.findByUsername("bluesky"));
	}
}
