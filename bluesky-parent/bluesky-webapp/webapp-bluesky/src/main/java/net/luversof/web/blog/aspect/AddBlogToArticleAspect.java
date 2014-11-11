package net.luversof.web.blog.aspect;

import java.util.List;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.BlueskyException;
import net.luversof.security.core.userdetails.BlueskyUser;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 인증이 들어간 경우 Article에 로그인한 유저의 blog객체를 추가 처리
 * 매개변수에 무조건 Article이 첫번째여야 하는 제한 조건은 변경을 할 수 없는건가..
 * @author choiyong-rak
 *
 */
@Aspect
@Component
public class AddBlogToArticleAspect {

	@Autowired
	private BlogService blogService;
	
	@Autowired
	private ArticleService articleService;
	
	@Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) && @annotation(net.luversof.web.blog.annotation.AddBlogToArticle) && args(blogId, article, ..)")
	public void beforeArticlePost(long blogId, Article article) {
		BlueskyUser blueskyUser = (BlueskyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		List<Blog> blogList = blogService.findByUser(blueskyUser.getId(), blueskyUser.getUserType().name());
		boolean checkBlog = false;
		for (Blog blog : blogList) {
			if (blog.getBlogId() == blogId) {
				checkBlog = true;
				article.setBlog(blog);
			}
		}
		if (!checkBlog) {
			throw new BlueskyException("blog.article.permissionDenied");
		}
	}
	
	@Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) && @annotation(net.luversof.web.blog.annotation.AddBlogToArticle) && args(blogId, article, articleId, ..)")
	public void beforeArticlePut(long blogId, Article article, long articleId) {
		Article targetArticle = articleService.findOne(articleId);
		
		if (targetArticle.getBlog().getBlogId() != blogId) {
			throw new BlueskyException("blog.article.notUserArticle");
		}
		
		article.setBlog(targetArticle.getBlog());
	}
}
