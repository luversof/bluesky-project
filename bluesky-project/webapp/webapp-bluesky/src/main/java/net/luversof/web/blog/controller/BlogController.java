package net.luversof.web.blog.controller;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.Blog.Modify;
import net.luversof.blog.domain.Blog.Save;
import net.luversof.blog.service.BlogCategoryService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.web.AuthorizeRole;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/blog")
public class BlogController {

	private static final int PAGE_BLOCK_SIZE = 10;

	@Autowired
	private BlogService blogService;

	@Autowired
	private BlogCategoryService blogCategoryService;

	@RequestMapping(StringUtils.EMPTY)
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
		return "/blog/list";
	}

	@RequestMapping("/{id}")
	public String view(@PathVariable long id, ModelMap modelMap) {
		modelMap.addAttribute(blogService.findOne(id));
		return "/blog/view";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping("/write")
	public void writePage(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[blog].username == authentication.name")
	@RequestMapping("/{id}/modify")
	public String modifyPage(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(blogService.findOne(id));
		modelMap.addAttribute(blogCategoryService.findByUsername(authentication.getName()));
		return "/blog/modify";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public void save(@Validated(Save.class) Blog blog, Authentication authentication, ModelMap modelMap) {
		if (blog.getBlogCategory() != null && blog.getBlogCategory().getId() != 0) {
			blog.setBlogCategory(blogCategoryService.findOne(blog.getBlogCategory().getId()));
		}
		modelMap.addAttribute("result", blogService.save(blog).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public void modify(@Validated(Modify.class) Blog blog, Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute("result", blogService.update(blog).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(@PathVariable long id, Authentication authentication, ModelMap modelMap) {
		Blog targetBlog = blogService.findOne(id);
		if (!authentication.getName().equals(targetBlog.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		blogService.delete(id);
	}
}
