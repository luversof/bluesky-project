package net.luversof.web;

import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import net.luversof.TestApplication;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.web.blog.controller.BlogController;

@WebMvcTest(BlogController.class)
@ContextConfiguration(classes = { TestApplication.class })
class SliceTest {
	
//	@Autowired
//	private MockMvc mvc;
	
	@MockBean
	private BlogController blogController;

	@Test
	void test() {
		given(blogController.userBlogList()).willReturn(List.of(new Blog()));
	}
}
