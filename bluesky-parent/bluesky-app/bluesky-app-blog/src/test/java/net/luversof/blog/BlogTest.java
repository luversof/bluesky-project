package net.luversof.blog;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.repository.mysql.BlogRepository;
import net.luversof.blog.service.BlogService;

@Slf4j
class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private SpringValidatorAdapter validator;
	
	@Test
	void validator() {
		Blog blog = new Blog();
		var result = validator.validate(blog, Blog.Create.class);
		log.debug("Test : {}", result);
	}
	
	@Test
	void createBlog() {
		Blog blog = new Blog();
		var result = blogService.createBlog(blog);
//		var result = blogService.createBlog("a", "b", blog);
//		var result = blogService.createBlog(blog, "a", "b");
		log.debug("result : {}", result);
	}
	
	@Test
	void saveTest() {
		Blog blog = new Blog();
		blog.setUserId(UUID.randomUUID().toString());
		blog.setBlogId(UUID.randomUUID().toString());
		blogRepository.save(blog);
		log.debug("blog : {}", blog);
	}
	
	@Test
	void findAll() {
		log.debug("findAll : {}", blogRepository.findAll());
	}

	@Test
	void findByUser() {
		Optional<Blog> blogOptional = blogRepository.findByUserId("77a04682-3032-492c-9449-5ba986491eef");
		log.debug("blogList : {}", blogOptional);
	}
	
	@Test
	void findById() {
		Blog blog = blogRepository.findById(1L).orElse(null);
		log.debug("blog : {}", blog);
	}
}
