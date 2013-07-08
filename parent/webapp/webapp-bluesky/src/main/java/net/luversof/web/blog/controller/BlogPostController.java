package net.luversof.web.blog.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.service.BlogPostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@Controller
@RequestMapping("/blogPost")
public class BlogPostController {
	
	@Autowired
	private BlogPostService blogPostService;
	
	@RequestMapping(value={"","/write"})
	public void page() {}
	
//	@RequestMapping
//	public String get() {
//		log.debug("blog get");
//		return "/blog/index";
//	}
	
	@RequestMapping(method=RequestMethod.POST)
	public void save(BlogPost blogPost) {
		log.debug("save blogPost : {}", blogPost);
		blogPostService.save(blogPost);
	}
	
	
	@RequestMapping("/list")
	public void list(ModelMap modelMap) {
		modelMap.addAttribute(blogPostService.list());
		log.debug("modelMap : {}", modelMap);
	}
	
	
	
	@RequestMapping("/{id}")
	public String view(@PathVariable Long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogPostService.view(id));
		log.debug("modelMap : {}", modelMap);
		return "/blogPost/view";
	}

}
