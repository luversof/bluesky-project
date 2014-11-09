package net.luversof.web.blog.controller;

import java.text.MessageFormat;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/blog")
public class BlogController {

	@Autowired
	private BlogService blogService;

	
	@RequestMapping(value = "/{blogId}", method=RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String forwardArticleList(@PathVariable long blogId) {
		return MessageFormat.format("redirect:/blog/{0}/article", blogId);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = { "/$!", "/$!/article" }, method=RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String create(Blog blog) {
		return forwardArticleList(blog.getId()); 
	}
	
	
	@RequestMapping(value = "/create", method=RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public void createForm() {}
}
