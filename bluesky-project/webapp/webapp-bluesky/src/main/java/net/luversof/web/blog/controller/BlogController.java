package net.luversof.web.blog.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.BlogCategoryService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/blog")
public class BlogController {
	
	private static final int PAGE_BLOCK_SIZE = 10;
	private static final String PRE_AUTHORIZE_ROLE = "hasRole('ROLE_USER')";

	@Autowired
	private BlogService blogService;

	@Autowired
	private BlogCategoryService blogCategoryService;

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = { "/write" })
	public void writePage(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST)
	public String save(Blog blog, Authentication authentication) {
		blog.setUsername(authentication.getName());
		log.debug("save blog : {}", blog);
		if (blog.getBlogCategory() != null && blog.getBlogCategory().getId() != 0) {
			blog.setBlogCategory(blogCategoryService.findOne(blog.getBlogCategory().getId()));
		}
		blogService.save(blog);
		return "redirect:/blog";
	}

	
	@RequestMapping(value = { "" })
	public String list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Page<Blog> blogPage = blogService.findAll(page - 1);
		if (blogPage.getTotalPages() > 0 && blogPage.getTotalPages() < page) {
			throw new BlueskyException("invalid page");
		}
		// 요청받은 page가 blogPage.getTotalPages()보다 큰 경우 예외처리가 필요
		int startPage = Math.max(1, page - (PAGE_BLOCK_SIZE / 2));
		int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, blogPage.getTotalPages());

		modelMap.addAttribute("pageImpl", blogPage);
		modelMap.addAttribute("currentPage", page);
		modelMap.addAttribute("startPage", startPage);
		modelMap.addAttribute("endPage", endPage);
		log.debug("modelMap : {}", modelMap);
		return "/blog/list";
	}

	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[blog].username == authentication.name")
	@RequestMapping("/{id}/modify")
	public String modifyPage(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogService.findOne(id));
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
		return "/blog/modify";
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public String modify(Blog blog, Authentication authentication) {
		Blog targetBlog = blogService.findOne(blog.getId());

		if (!authentication.getName().equals(targetBlog.getUsername())) {
			throw new BlueskyException("username is not owner");
		}

		targetBlog.setTitle(blog.getTitle());
		targetBlog.setContent(blog.getContent());
		if (blog.getBlogCategory() != null && blog.getBlogCategory().getId() != 0) {
			targetBlog.setBlogCategory(blogCategoryService.findOne(blog.getBlogCategory().getId()));
		}
		blogService.save(targetBlog);
		return "redirect:/blog/" + blog.getId();
	}

	@RequestMapping("/{id}")
	public String view(@PathVariable long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogService.findOne(id));
		log.debug("modelMap : {}", modelMap);
		return "/blog/view";
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		Blog targetBlog = blogService.findOne(id);
		if (!authentication.getName().equals(targetBlog.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", id);
		blogService.delete(id);
		return "redirect:/blog";
	}
}
