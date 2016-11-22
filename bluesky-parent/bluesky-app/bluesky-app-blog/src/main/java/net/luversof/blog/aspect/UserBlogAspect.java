package net.luversof.blog.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import net.luversof.blog.annotation.UserBlog;
import net.luversof.blog.domain.Blog;

//@Aspect
//@Component
public class UserBlogAspect {


	//@Before("execution(public * *(@net.luversof.blog.annotation.UserBlog (*))) && args(blog) && bean(*Controller)")	// 일단 이건 단일 호출시 동작함
	public void methodBefore(JoinPoint joinPoint/*, UserBlog userBlog*/, Blog blog) {
		blog.setId(123);
	}
}
