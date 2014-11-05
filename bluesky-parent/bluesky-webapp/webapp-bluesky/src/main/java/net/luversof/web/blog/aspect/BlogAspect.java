package net.luversof.web.blog.aspect;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.security.core.userdetails.BlueskyUser;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * blog.username을 spring-security의 authentication으로 부터 설정
 * blog와 authentication 객체가 controller에 매개변수로 넘어오는 경우 authentication의 name 을 blog의 username으로 설정
 * @author bluesky
 *
 */
@Slf4j
@Aspect
@Component
public class BlogAspect {
	
	@Autowired
	private BlogService blogService;

	@Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) && execution(* net.luversof.web.blog.controller..*(..)) && args(article,authentication,..)")
	public void classPointcut(Article article, Authentication authentication) {
	}
	
	@Before("classPointcut(article, authentication)")
	public void beforeClassPointcut(Article article, Authentication authentication) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		log.debug("[blog] object set userId, userId : {}", blueskyUser.getId());
		if (article.getBlog() == null) {
			List<Blog> blogList = blogService.findByUser(blueskyUser.getId(), blueskyUser.getUserType().name());
			article.setBlog(blogList.get(0));
		}
	}
}
