package net.luversof.web.blog.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/blog")
public class BlogController {
	
	@Autowired
	private BlogService blogService;
	
	@RequestMapping
	public String get() {
		log.debug("blog get");
		return "/blog/index";
	}
	
	
	@RequestMapping("/list")
	public void list(ModelMap modelMap) {
		modelMap.addAttribute(blogService.list());
	}
	
	@RequestMapping("/{id}")
	public void view(Long id, ModelMap modelMap) {
		modelMap.addAttribute(blogService.view(id));
	}

}
