package net.luversof.web.gate.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import net.luversof.web.gate.blog.httpexchange.BlogArticleCategoryClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleClient;
import net.luversof.web.gate.blog.httpexchange.BlogArticleCommentClient;
import net.luversof.web.gate.blog.httpexchange.BlogClient;

@Configuration
@ImportHttpServices(group = "client-blog", types = { 
		BlogArticleCategoryClient.class,
		BlogArticleClient.class,
		BlogArticleCommentClient.class,
		BlogClient.class,
})
public class GateBlogConfig {

}
