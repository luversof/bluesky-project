package net.luversof.web.blog.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.service.BlogCategoryService;
import net.luversof.blog.service.BlogPostService;
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
@RequestMapping("/blogPost")
public class BlogPostController {
	
	private static final int PAGE_BLOCK_SIZE = 10;
	private static final String PRE_AUTHORIZE_ROLE = "hasRole('ROLE_USER')";

	@Autowired
	private BlogPostService blogPostService;

	@Autowired
	private BlogCategoryService blogCategoryService;

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = { "/write" })
	public void writePage(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST)
	public String save(BlogPost blogPost, Authentication authentication) {
		blogPost.setUsername(authentication.getName());
		log.debug("save blogPost : {}", blogPost);
		if (blogPost.getBlogCategory() != null && blogPost.getBlogCategory().getId() != 0) {
			blogPost.setBlogCategory(blogCategoryService.findOne(blogPost.getBlogCategory().getId()));
		}
		blogPostService.save(blogPost);
		return "redirect:/blogPost";
	}

	
	@RequestMapping(value = { "" })
	public String list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Page<BlogPost> blogPostPage = blogPostService.findAll(page - 1);
		if (blogPostPage.getTotalPages() > 0 && blogPostPage.getTotalPages() < page) {
			throw new BlueskyException("invalid page");
		}
		// 요청받은 page가 blogPostPage.getTotalPages()보다 큰 경우 예외처리가 필요
		int startPage = Math.max(1, page - (PAGE_BLOCK_SIZE / 2));
		int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, blogPostPage.getTotalPages());

		modelMap.addAttribute("pageImpl", blogPostPage);
		modelMap.addAttribute("currentPage", page);
		modelMap.addAttribute("startPage", startPage);
		modelMap.addAttribute("endPage", endPage);
		log.debug("modelMap : {}", modelMap);
		return "/blogPost/list";
	}

	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[blogPost].username == authentication.name")
	@RequestMapping("/{id}/modify")
	public String modifyPage(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogPostService.findOne(id));
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
		return "/blogPost/modify";
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public String modify(BlogPost blogPost, Authentication authentication) {
		BlogPost targetBlogPost = blogPostService.findOne(blogPost.getId());

		if (!authentication.getName().equals(targetBlogPost.getUsername())) {
			throw new BlueskyException("username is not owner");
		}

		targetBlogPost.setTitle(blogPost.getTitle());
		targetBlogPost.setContent(blogPost.getContent());
		if (blogPost.getBlogCategory() != null && blogPost.getBlogCategory().getId() != 0) {
			targetBlogPost.setBlogCategory(blogCategoryService.findOne(blogPost.getBlogCategory().getId()));
		}
		blogPostService.save(targetBlogPost);
		return "redirect:/blogPost/" + blogPost.getId();
	}

	@RequestMapping("/{id}")
	public String view(@PathVariable long id, ModelMap modelMap) {
		log.debug("id : {}", id);
		modelMap.addAttribute(blogPostService.findOne(id));
		log.debug("modelMap : {}", modelMap);
		return "/blogPost/view";
	}

	@PreAuthorize(PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		BlogPost targetBlogPost = blogPostService.findOne(id);
		if (!authentication.getName().equals(targetBlogPost.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", id);
		blogPostService.delete(id);
		return "redirect:/blogPost";
	}
}
