package net.luversof.web.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Blog;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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

	@Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) && execution(* net.luversof.web.blog.controller..*(..)) && args(blog,authentication,..)")
	public void classPointcut(Blog blog, Authentication authentication) {
	}
	
	@Before("classPointcut(blog, authentication)")
	public void beforeClassPointcut(Blog blog, Authentication authentication) {
		log.debug("[blog] object set username, username : {}", authentication.getName());
		if (StringUtils.isEmpty(blog.getUsername())) {
			blog.setUsername(authentication.getName());
		}
	}
}
