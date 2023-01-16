//package net.luversof.web.blog.controller;
//
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.http.MediaType;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
//import io.swagger.v3.oas.annotations.Operation;
//import net.luversof.blog.domain.mysql.BlogArticle;
//import net.luversof.blog.service.BlogArticleService;
//import net.luversof.security.util.SecurityUtil;
//import net.luversof.web.blog.domain.BlogArticlePageRequest;
//
//@RestController
//@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
//public class BlogArticleController {
//
//	@Autowired
//	private BlogArticleService blogArticleService;
//	
//	@Operation(summary = "블로그 글 목록 조회")
//	@GetMapping("/search/findByBlogId/{blogId}")
//	public Page<BlogArticle> findByBlogId(@PathVariable String blogId, BlogArticlePageRequest blogArticlePageRequest) {
//		return blogArticleService.findByBlogId(blogId, blogArticlePageRequest.toPageRequest());
//	}
//	
//	
//	@Operation(summary = "블로그 글 조회")
//	@GetMapping("/{blogArticleId}")
//	public Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId) {
//		
//		var savedBlogArticle = blogArticleService.findByBlogArticleId(blogArticleId);
//		// TODO blogArticle 조회수 증가 처리
//		return savedBlogArticle;
//	}
//	
//	@Operation(summary = "블로그 글 생성")
//	@BlueskyPreAuthorize
//	@PostMapping
//	public BlogArticle create(@RequestBody @Validated(BlogArticle.CreateParam.class) BlogArticle blogArticle) {
//		blogArticle.setUserId(SecurityUtil.getBlueskyUser().getId());
//		return blogArticleService.create(blogArticle);
//	}
//	
//	@Operation(summary = "블로그 글 수정")
//	@BlueskyPreAuthorize
//	@PutMapping("/{blogArticleId}")
//	public BlogArticle update(@RequestBody @Validated(BlogArticle.UpdateParam.class) BlogArticle blogArticle) {
//		blogArticle.setUserId(SecurityUtil.getBlueskyUser().getId());
//		return blogArticleService.update(blogArticle);
//	}
//	
//	@Operation(summary = "블로그 글 삭제")
//	@BlueskyPreAuthorize
//	@DeleteMapping("/{blogArticleId}")
//	public void delete(@Validated(BlogArticle.DeleteParam.class) BlogArticle blogArticle) {
//		blogArticle.setUserId(SecurityUtil.getBlueskyUser().getId());
//		blogArticleService.delete(blogArticle);
//	}
//
//}
