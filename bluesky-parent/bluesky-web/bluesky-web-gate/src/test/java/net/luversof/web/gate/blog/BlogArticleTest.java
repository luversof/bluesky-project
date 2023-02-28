package net.luversof.web.gate.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.gate.blog.client.BlogArticleClient;
import net.luversof.web.gate.blog.domain.BlogArticle;

class BlogArticleTest implements GeneralWebTest {

	@Autowired
	private BlogArticleClient blogArticleClient;
	
	@Test
	void delete() {
		var blogArticle = BlogArticle.builder().blogArticleId("TEST").userId("user").build();
		
		blogArticleClient.delete(blogArticle);
		assertThat(blogArticle).isNotNull();
	}
	
}
