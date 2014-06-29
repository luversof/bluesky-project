package net.luversof.web.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Article;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

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

	@Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) && execution(* net.luversof.web.blog.controller..*(..)) && args(article,authentication,..)")
	public void classPointcut(Article article, Authentication authentication) {
	}
	
	@Before("classPointcut(blog, authentication)")
	public void beforeClassPointcut(Article article, Authentication authentication) {
		log.debug("[blog] object set username, username : {}", authentication.getName());
		if (article.getBlog() == null) {
			article.setBlog(blog);
		}
	}
}
