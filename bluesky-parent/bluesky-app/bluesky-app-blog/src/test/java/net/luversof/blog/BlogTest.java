package net.luversof.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.service.BlogService;

@Slf4j
class BlogTest extends GeneralTest {
	
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
	@DisplayName("blog 생성")
	void createBlog() {
		Blog blog = new Blog();
		blog.setUserId(BlogTestConstant.USER_ID);
		var result = blogService.createBlog(blog);
		assertThat(result).isNotNull();
		log.debug("result : {}", result);
	}

}