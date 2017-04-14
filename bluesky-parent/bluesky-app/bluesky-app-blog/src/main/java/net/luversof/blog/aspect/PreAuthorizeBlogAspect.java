package net.luversof.blog.aspect;

import java.util.Arrays;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.TypeUtils;

import net.luversof.blog.annotation.PreAuthorizeBlog;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.exception.BlogErrorCode;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.core.exception.BlueskyException;

/**
 * Blog 매개변수에 대한 검증을 하는 aop
 * @author bluesky
 *
 */
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
			throw new BlueskyException("blog.NOT_EXIST_PARAMETER_BOARD_ALIAS");
		}
		
		Blog targetBlog = null;
		
		for (int i = 0 ; i < signature.getParameterTypes().length ; i++) {
			if (TypeUtils.isAssignable(Blog.class, signature.getParameterTypes()[i])) {
				targetBlog = (Blog) joinPoint.getArgs()[i];
				break;
			}
		}
		
		if (targetBlog == null) {
			throw new BlueskyException("blog.NOT_EXIST_BLOG_TYPE_PARAMETER");
		}
		
		checkBlogOwner(preAuthorizeBlog, targetBlog);
	}
	
	
	private void checkBlogOwner(PreAuthorizeBlog preAuthorizeBlog, Blog targetBlog) {
		if (!preAuthorizeBlog.checkBlogOwner()) {
			return;
		}
		
		List<Blog> userBlog = BlogRequestAttributeUtil.getUserBlogList();
		if (userBlog.isEmpty()) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG);
		}
		
		if (userBlog.stream().noneMatch(blog -> blog.getId() == targetBlog.getId())) {
			throw new BlueskyException("blog.NOT_USER_BLOG");
		}
	}
}
