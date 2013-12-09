package net.luversof.blog;


import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogCategory;
import net.luversof.blog.domain.QBlog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.service.BlogCategoryService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.config.GeneralTest;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Slf4j
public class BlogTest extends GeneralTest {

	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogCategoryService blogCategoryService;
	

	@Test
	@Ignore
	public void 셀렉트테스트() {
		Blog blog = blogService.findOne(28);
		log.debug("result : {}", blog);
	}

	@Test
	@Ignore
	public void save테스트() {
		Blog blog = new Blog();
		blog.setUsername("test");
		blog.setTitle("한글제목");
		blog.setContent("한글내용");
		
		BlogCategory blogCategory = blogCategoryService.findOne(1);
		blog.setBlogCategory(blogCategory);

		Blog savedBlog = blogService.save(blog);
		log.debug("savedBlog : {}", savedBlog);
		log.debug("savedBlog : {}", savedBlog.getId());
	}
	
	@Test
//	@Ignore
	public void 대량save테스트() {
		for (int i = 0 ; i < 11 ; i++) {
			save테스트();
		}
	}

	@Test
//	@Ignore
	public void selectPaging테스트() {
		Page<Blog> blogList = blogService.findAll(0);
		log.debug("blogList : {}", blogList);
		log.debug("blogList : {}", blogList.getContent());
	}
	
	@Test
	@Ignore
	public void 테스트() {
		QBlog blog = QBlog.blog;
		Iterable<Blog> list = blogRepository.findAll(blog.content.startsWith("c"));
		log.debug("list : {}", list);
	}
	
	@Test
	public void 블로그카테고리테스트() {
		List<BlogCategory> list = blogCategoryService.findByUsername("bluesky");
		
		
		log.debug("list : {}", list);
	}
}
