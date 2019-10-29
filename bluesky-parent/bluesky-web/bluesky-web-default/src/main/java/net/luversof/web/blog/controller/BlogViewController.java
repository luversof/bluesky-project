package net.luversof.web.blog.controller;

import java.text.MessageFormat;

import javax.annotation.Resource;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;

@Controller
@RequestMapping(value = "/blog", produces = MediaType.TEXT_HTML_VALUE)
public class BlogViewController {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;

	/**
	 * 진입 페이지 blog 정보가 없는 경우 생성 페이지로 이동
	 * 
	 * @param blog
	 * @return
	 */
	@BlueskyPreAuthorize
	@GetMapping
	public String home() {
		Blog userBlog = BlogRequestAttributeUtil.getUserBlog();
		if (userBlog == null) {
			return "redirect:/blog/create";
		}
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), userBlog.getId()));
	}

	@GetMapping(value = "/create")
	public void createForm() {
	}
}
