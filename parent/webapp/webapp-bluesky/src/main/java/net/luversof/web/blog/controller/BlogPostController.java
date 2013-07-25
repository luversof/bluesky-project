package net.luversof.web.blog.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.service.BlogPostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/blogPost")
public class BlogPostController {

	@Autowired
	private BlogPostService blogPostService;
	
	@RequestMapping(value = { "", "/write" })
	public void page() {
	}

	@RequestMapping(method = RequestMethod.POST)
	public String save(BlogPost blogPost) {
		log.debug("save blogPost : {}", blogPost);
		blogPostService.save(blogPost);
		return "redirect:/blogPost/list";
	}

	@RequestMapping("/list")
	public void list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Page<BlogPost> blogPostPage = blogPostService.list(page - 1);
		//요청받은 page가 blogPostPage.getTotalPages()보다 큰 경우 예외처리가 필요
		int startPage = Math.max(1, page - 5);
		int endPage = Math.min(startPage + 10 - 1, blogPostPage.getTotalPages());

		modelMap.addAttribute("pageImpl", blogPostPage);
		modelMap.addAttribute("currentPage", page);
		modelMap.addAttribute("startPage", startPage);
		modelMap.addAttribute("endPage", endPage);
		log.debug("modelMap : {}", modelMap);
	}

	@RequestMapping("/{id}/modify")
	public String modifyPage(@PathVariable long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogPostService.view(id));
		return "/blogPost/modify";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public String modify(BlogPost blogPost) {
		blogPostService.save(blogPost);
		return "redirect:/blogPost/" + blogPost.getId();
	}

	@RequestMapping("/{id}")
	public String view(@PathVariable long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogPostService.view(id));
		log.debug("modelMap : {}", modelMap);
		return "/blogPost/view";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		blogPostService.delete(id);
		return "redirect:/blogPost/list";
	}

}
