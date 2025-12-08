package net.luversof.web.gate.blog.httpexchange;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.blog.domain.BlogArticle;

@HttpExchange(url = "/api/blogArticle")
public interface BlogArticleClient {

	@PostExchange
	BlogArticle create(@RequestBody BlogArticle blogArticle);
	
	@GetExchange("/search/findByBlogId/{blogId}")
	Page<BlogArticle> findByBlogId(@PathVariable String blogId, Pageable pageable);
	
	@GetExchange("/search/findByBlogArticleId/{blogArticleId}")
	Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId);
	
	@PutExchange
	BlogArticle update(@RequestBody BlogArticle blogArticle);
	
	@DeleteExchange
	void delete(@RequestBody BlogArticle blogArticle);

}