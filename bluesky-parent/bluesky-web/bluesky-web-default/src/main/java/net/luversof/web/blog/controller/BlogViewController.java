package net.luversof.web.blog.controller;

import java.text.MessageFormat;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.blog.annotation.UserBlog;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.domain.BlogArticle.Get;
import net.luversof.blog.service.BlogArticleCategoryService;
import net.luversof.blog.service.BlogArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@Controller
@RequestMapping(value = "/blog"/*, produces = MediaType.TEXT_HTML_VALUE*/)
public class BlogViewController {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;

	@Autowired
	private BlogService blogService;

	@Autowired
	private BlogArticleService blogArticleService;

	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;

	/**
	 * 진입 페이지 blog 정보가 없는 경우 생성 페이지로 이동
	 * 
	 * @param blog
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@GetMapping(value = "")
	public String home(@UserBlog Blog blog) {
		return redirectBlogArticleList(blog.getId());
	}

	@GetMapping(value = { "/{blogId}", "/{blogId}/view" })
	public String redirectBlogArticleList(@PathVariable long blogId) {
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), blogId));
	}

	@GetMapping(value = "/create")
	public void createForm() {
	}

	/**
	 * 생성 후 해당 페이지 이동
	 * 
	 * @param authentication
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostMapping(value = "")
	public String create(Authentication authentication) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		List<Blog> savedBlogList = blogService.findByUser(blueskyUser.getId());
		if (!savedBlogList.isEmpty()) {
			return redirectBlogArticleList(savedBlogList.get(0).getId());
		}
		;
		Blog blog = new Blog();
		blog.setUserId(blueskyUser.getId());
		blogService.save(blog);
		return redirectBlogArticleList(blog.getId());
	}

	/**
	 * 글 목록
	 * 
	 * @param blogId
	 * @param page
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/{blogId}/list")
	public String list(@PathVariable long blogId, @PageableDefault(sort = { "id" }, direction = Direction.DESC) Pageable pageable, ModelMap modelMap) {
		Blog blog = blogService.findOne(blogId);
		Page<BlogArticle> blogArticlePage = blogArticleService.findByBlog(blog, pageable);
		modelMap.addAttribute("blogArticlePage", blogArticlePage);
		return "blog/list";
	}

	/**
	 * 글 보기
	 * 
	 * @param blogArticle
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/{blog.id}/view/{id}")
	public String view(@Validated(Get.class) BlogArticle blogArticle, ModelMap modelMap) {
		BlogArticle viewArticle = blogArticleService.findById(blogArticle.getId()).get();
		blogArticleService.incraseViewCount(viewArticle);
		modelMap.addAttribute("blogArticle", viewArticle);
		return "blog/view";
	}

	/**
	 * 글 쓰기
	 * 
	 * @param blog
	 * @param modelMap
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@GetMapping(value = "/{id}/write")
	public String writePage(@UserBlog(checkBlog = true) Blog blog, ModelMap modelMap) {
		modelMap.addAttribute(blogArticleCategoryService.findByBlog(blog));
		return "blog/write";
	}

	/**
	 * 글 수정
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER')")
	@GetMapping(value = "/{blog.id}/modify/{id}")
	public String modifyPage(@UserBlog(checkBlog = true) @Validated(Get.class) BlogArticle article, ModelMap modelMap) {
		modelMap.addAttribute(blogArticleService.findById(article.getId()));
		modelMap.addAttribute(blogArticleCategoryService.findByBlog(blogService.findOne(article.getId())));
		return "blog/modify";
	}
}
