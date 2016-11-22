package net.luversof.blog;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;

@Slf4j
public class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogService blogService;
	
	@Test
	public void saveTest() {
		Blog blog = new Blog();
		blog.setUserId("1");
		blogService.save(blog);
	}

	@Test
	public void test() {
		List<Blog> blogList = blogService.findByUser("1657880612");
		log.debug("blogList : {}", blogList);
	}
}
