package net.luversof.blog;

import java.util.Optional;
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
		Blog blog = blogService.create();
		log.debug("blog : {}", blog);
	}
	
	@Test
	public void findAll() {
		log.debug("findAll : {}", blogRepository.findAll());
	}

	@Test
	public void findByUser() {
		Optional<Blog> blogOptional = blogService.findByUserId(UUID.fromString("77a04682-3032-492c-9449-5ba986491eef"));
		log.debug("blogList : {}", blogOptional);
	}
	
	@Test
	public void findById() {
		UUID uuid = UUID.fromString("745B5D33-E9A1-4398-B997-BC3D6B83BF8B");
		Blog blog = blogService.findById(uuid).orElse(null);
		log.debug("blog : {}", blog);
	}
}
