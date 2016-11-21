package net.luversof.web.blog.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.TypeUtils;

import net.luversof.blog.domain.Blog;
import net.luversof.core.exception.BlueskyException;
import net.luversof.web.blog.annotation.PreAuthorizeBlog;

@Aspect
@Component
public class PreAuthorizeBlogAspect {

	@Before("@annotation(preAuthorizeBlog)")
	public void methodBefore(JoinPoint joinPoint, PreAuthorizeBlog preAuthorizeBlog) {
		checkBefore(joinPoint, preAuthorizeBlog);
	}
	
	@Before("@target(preAuthorizeBlog) && bean(*Controller)")
	public void classBefore(JoinPoint joinPoint, PreAuthorizeBlog preAuthorizeBlog) {
		checkBefore(joinPoint, preAuthorizeBlog);
	}
	
	
	public void checkBefore(JoinPoint joinPoint, PreAuthorizeBlog preAuthorizeBlog) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		if (Arrays.stream(signature.getParameterTypes()).noneMatch(parameterType -> TypeUtils.isAssignable(Blog.class, parameterType))) {
			throw new BlueskyException("snow3.NOT_EXIST_PARAMETER_BOARD_ALIAS");
		}
		
		Blog blog = null;
		
		for (int i = 0 ; i < signature.getParameterTypes().length ; i++) {
			if (TypeUtils.isAssignable(Blog.class, signature.getParameterTypes()[i])) {
				blog = (Blog) joinPoint.getArgs()[i];
				break;
			}
		}
		
		if (blog == null) {
			throw new BlueskyException("");
		}
		
		checkBlogOwner(preAuthorizeBlog);
	}
	
	
	private void checkBlogOwner(PreAuthorizeBlog preAuthorizeBlog) {
		if (!preAuthorizeBlog.checkBlogOwner()) {
			return;
		}
		
	}
}
