package net.luversof.blog;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;

@Slf4j
public class BlogTest extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
//	@Test
//	public void saveTest() {
//		Blog blog = blogRepository.create();
//		log.debug("blog : {}", blog);
//	}
	
	@Test
	public void findAll() {
		log.debug("findAll : {}", blogRepository.findAll());
	}

	@Test
	public void findByUser() {
		Optional<Blog> blogOptional = blogRepository.findByUserId(UUID.fromString("77a04682-3032-492c-9449-5ba986491eef"));
		log.debug("blogList : {}", blogOptional);
	}
	
	@Test
	public void findById() {
		UUID uuid = UUID.fromString("745B5D33-E9A1-4398-B997-BC3D6B83BF8B");
		Blog blog = blogRepository.findById(uuid).orElse(null);
		log.debug("blog : {}", blog);
	}
}
