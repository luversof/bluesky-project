package net.luversof.web;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.user.repository.UserRepository;

@Slf4j
public class TestClass extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Test
	@Transactional
	public void test(){
	}
}
