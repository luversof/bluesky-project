package net.luversof.blog;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogService blogService;
	
	@Test
	public void saveTest() {
		Blog blog = new Blog();
		blog.setUserId(12345);
		blogService.save(blog);
	}

	@Test
	public void test() {
		Blog blog = blogService.findByUser(1657880612);
		log.debug("blog : {}", blog);
	}
}
