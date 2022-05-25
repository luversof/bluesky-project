package net.luversof.blog;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.repository.mysql.BlogRepository;

@Slf4j
class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
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
