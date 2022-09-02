package net.luversof.blog.controller;

import java.util.Optional;

import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import lombok.Data;
import net.luversof.blog.controller.BlogArticleController.BlogArticlePageRequest;
import net.luversof.blog.domain.mysql.BlogArticle;
import net.luversof.blog.service.BlogArticleService;

@Controller("blogArticleControllerForGraphQL")
public class BlogArticleController {
	
	@Autowired
	private BlogArticleService blogArticleService;

	@QueryMapping
	public Page<BlogArticle> findByBlogId(@Argument String blogId, BlogArticlePageRequest blogArticlePageRequest) {
		return blogArticleService.findByBlogId(blogId, blogArticlePageRequest.toPageRequest());
	}
	
	@Data
	public class BlogArticlePageRequest {
		
		@Range(min = 0)
		private int page;
		
		@Range(min = 1, max = 100)
		private int size = 10;
		
		private Sort sort = Sort.by("idx").descending();

		public PageRequest toPageRequest() {
			return PageRequest.of(page, size, sort);
		}

	}
	
	@QueryMapping
	public Optional<BlogArticle> findByBlogArticleId(@Argument String blogArticleId) {
		var savedBlogArticle = blogArticleService.findByBlogArticleId(blogArticleId);
		// TODO blogArticle 조회수 증가 처리
		return savedBlogArticle;
	}
	
}
