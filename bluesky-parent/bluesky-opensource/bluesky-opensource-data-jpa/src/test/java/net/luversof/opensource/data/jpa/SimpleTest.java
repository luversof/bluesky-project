package net.luversof.opensource.data.jpa;

import lombok.extern.slf4j.Slf4j;
import net.luversof.opensource.data.jpa.domain.Blog;
import net.luversof.opensource.data.jpa.domain.User;
import net.luversof.opensource.data.jpa.service.BlogService;
import net.luversof.opensource.data.jpa.service.UserService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class SimpleTest extends GeneralTest {
	
	@Value("${datasource.blog.username}")
	private String username;
	
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private UserService userService;
	
	
	@Test
	public void insertTest() {
		User user = new User();
		user.setEnable(true);
		user.setPassword("Test");
		user.setUsername("username4");
		
		User userResult = userService.save(user);
		log.debug("user : {}", user);
		log.debug("userResult : {}", userResult);
		
	}
	
	@Test
	public void getTest() {
		User user = userService.findOne(1);
		log.debug("user : {}", user);
	}

	@Test
	public void testDefaultSettings() throws Exception {
		System.out.println(":::"  + username);
		
		Blog blog = blogService.findOne(1);
		log.debug("blog : {}", blog);
		
		
		User user = userService.findOne(1);
		log.debug("user : {}", user);
//		ResultSet resultSet = blogDataSource.getConnection().createStatement().executeQuery("SELECT * FROM Blog where id = 1");
//		System.out.println(resultSet);
//		
//		ResultSet resultSet2 = memberDataSource.getConnection().createStatement().executeQuery("SELECT * FROM User where id = 1");
//		System.out.println(resultSet2);
	}
	
	
}
