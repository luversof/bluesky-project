package net.luversof.web.blog.aspect;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.security.core.userdetails.BlueskyUser;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 인증이 필수인 요청에 대해 요청받은 blogId가 해당 유저의 blog인지 체크  Article에 blog 객체를 추가하는 aop
 * 매개변수에 무조건 Article이 첫번째여야 하는 제한 조건은 변경을 할 수 없는건가..
 * @author luversof
 *
 */
@Aspect
@Component
public class CheckBlogAndAddToArticleAspect {

	@Autowired 
	private BlogService blogService;
	
	@Autowired
	private ArticleService articleService;
	
	@Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) && execution( * *(@net.luversof.web.blog.annotation.CheckBlogAndAddToArticle (*), ..)) && args(article, ..)")
//	@Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) && @annotation(net.luversof.web.blog.annotation.CheckBlogAndAddToArticle) && args(article, ..)")
	public void before(Article article) {
		Blog targetBlog = blogService.findOne(article.getBlog().getId());
		
		BlueskyUser blueskyUser = (BlueskyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (targetBlog.getUserId() != blueskyUser.getId() || !targetBlog.getUserType().equals(blueskyUser.getUserType().name())) {
			throw new BlueskyException("blog.invalidAccess");
		}
		
		// 수정 요청인 경우 저장된 글에서 blog 정보를 호출하여 한번 더 체크
		if (article.getId() != 0) {
			Article targetArticle = articleService.findOne(article.getId());
			if (targetArticle.getBlog().getId() != article.getBlog().getId()) {
				throw new BlueskyException("blog.article.invalidAccess");
			}
			if (!targetBlog.equals(targetArticle.getBlog())) {
				throw new BlueskyException("blog.article.invalidAccess");
			}
		}
		article.setBlog(targetBlog);
	}
}
