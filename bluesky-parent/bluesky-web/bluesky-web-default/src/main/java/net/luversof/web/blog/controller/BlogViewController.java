package net.luversof.web.blog.controller;

import java.text.MessageFormat;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.Article.Get;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.blog.annotation.CheckBlog;
import net.luversof.web.blog.annotation.CheckBlogAndAddToArticle;
import net.luversof.web.constant.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/blog"/*, produces = MediaType.TEXT_HTML_VALUE*/)
public class BlogViewController {

	private static final int PAGE_BLOCK_SIZE = 10;

	@Autowired
	private BlogService blogService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	/**
	 * 진입 페이지 blog 정보가 없는 경우 생성 페이지로 이동
	 * 
	 * @param blog
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@GetMapping(value = { "/$!", "/$!/article" })
	public String home(Blog blog) {
		return redirectArticleList(blog.getId());
	}

	@GetMapping(value = "/{blog.id}")
	public String redirectArticleList(@PathVariable("blog.id") long blogId) {
		return MessageFormat.format("redirect:/blog/{0}/article", blogId);
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
		Blog savedBlog = blogService.findByUser(blueskyUser.getId());
		if (savedBlog != null) {
			return redirectArticleList(savedBlog.getId());
		}
		;
		Blog blog = new Blog();
		blog.setUserId(blueskyUser.getId());
		blogService.save(blog);
		return redirectArticleList(blog.getId());
	}

	/**
	 * 글 목록
	 * 
	 * @param blogId
	 * @param page
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/{blog.id}/article")
	public String list(@PathVariable("blog.id") long blogId, @RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Blog blog = blogService.findOne(blogId);
		Page<Article> articlePage = articleService.findByBlog(blog, page - 1);
		if (articlePage.getTotalPages() > 0 && articlePage.getTotalPages() < page) {
			throw new BlueskyException("invalid page");
		}
		// 요청받은 page가 blogPage.getTotalPages()보다 큰 경우 예외처리가 필요
		int startPage = Math.max(1, page - (PAGE_BLOCK_SIZE / 2));
		int endPage = Math.min(startPage + PAGE_BLOCK_SIZE - 1, articlePage.getTotalPages());

		modelMap.addAttribute("pageImpl", articlePage);
		modelMap.addAttribute("currentPage", page);
		modelMap.addAttribute("startPage", startPage);
		modelMap.addAttribute("endPage", endPage);
		return "blog/article/list";
	}

	/**
	 * 글 보기
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/{blog.id}/article/{id}")
	public String view(@Validated(Get.class) Article article, ModelMap modelMap) {
		Article viewArticle = articleService.findOne(article.getId());
		articleService.incraseViewCount(viewArticle);
		modelMap.addAttribute("article", viewArticle);
		return "blog/article/view";
	}

	/**
	 * 글 쓰기
	 * 
	 * @param blog
	 * @param modelMap
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@GetMapping(value = "/{blog.id}/article/write")
	public String writePage(@CheckBlog Blog blog, ModelMap modelMap) {
		modelMap.addAttribute(articleCategoryService.findByBlog(blog));
		return "blog/article/write";
	}

	/**
	 * 글 수정
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[article].blog.userId == authentication.principal.id")
	@GetMapping(value = "/{blog.id}/article/{id}/modify")
	public String modifyPage(@CheckBlogAndAddToArticle @Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		modelMap.addAttribute(articleCategoryService.findByBlog(blogService.findOne(article.getId())));
		return "blog/article/modify";
	}
}
