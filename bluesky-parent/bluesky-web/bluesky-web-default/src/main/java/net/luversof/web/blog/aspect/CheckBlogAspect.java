package net.luversof.web.blog.aspect;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.security.core.userdetails.BlueskyUser;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CheckBlogAspect {

	@Autowired
	private BlogService blogService;
	
	@Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) && execution( * *(@net.luversof.web.blog.annotation.CheckBlog (*), ..)) && args(blog, ..)")
	public void before(Blog blog) {
		Blog targetBlog = blogService.findOne(blog.getId());
		
		BlueskyUser blueskyUser = (BlueskyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (targetBlog.getUserId() != blueskyUser.getId() || !targetBlog.getUserType().equals(blueskyUser.getUserType().name())) {
			throw new BlueskyException("blog.invalidAccess");
		}
		blog = targetBlog;
	}

}
