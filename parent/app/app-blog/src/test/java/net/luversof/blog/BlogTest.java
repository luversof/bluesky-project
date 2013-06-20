package net.luversof.blog;


import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.core.config.GeneralTest;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
public class BlogTest extends GeneralTest {

	@Autowired
	private BlogRepository blogRepository;

	@Test
	@Ignore
	public void 셀렉트테스트() {
		Blog blog = blogRepository.findOne((long) 1);
		log.debug("result : {}", blog);
	}

	@Test
//	@Ignore
	public void save테스트() {
		Blog blog = new Blog();
		blog.setMemberId(1);
		blog.setTitle("title");
		blog.setContent("content");

		Blog savedBlog = blogRepository.save(blog);
		log.debug("savedBlog : {}", savedBlog);
	}

	@Test
	@Ignore
	public void selectPaging테스트() {
		Pageable pageable = new PageRequest(1, 20);
		Page<Blog> blogPage = blogRepository.findAll(pageable);
		log.debug("blogPage : {}", blogPage);
	}
}