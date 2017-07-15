package net.luversof.blog;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.service.BlogService;

@Slf4j
public class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Test
	public void saveTest() {
		Blog blog = new Blog();
		blogService.save(blog);
		log.debug("blog : {}", blog);
	}
	
	@Test
	public void findAll() {
		log.debug("findAll : {}", blogRepository.findAll());
	}

	@Test
	public void findByUser() {
		List<Blog> blogList = blogService.findByUserId("1657880612");
		log.debug("blogList : {}", blogList);
	}
	
	@Test
	public void findById() {
		UUID uuid = UUID.fromString("745B5D33-E9A1-4398-B997-BC3D6B83BF8B");
		Blog blog = blogService.findById(uuid).orElse(null);
		log.debug("blog : {}", blog);
	}
}
