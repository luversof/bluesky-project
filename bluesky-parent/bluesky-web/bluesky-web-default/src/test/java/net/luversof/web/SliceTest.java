package net.luversof.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import net.luversof.TestApplication;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.web.blog.controller.BlogController;

@WebMvcTest(BlogController.class)
@ContextConfiguration(classes = { TestApplication.class })
public class SliceTest {
	
	@Autowired
	private MockMvc mvc;
	
	@MockBean
	private BlogController blogController;

	@Test
	void test() {
		given(blogController.userBlogList(any())).willReturn(List.of(new Blog()));
	}
}
