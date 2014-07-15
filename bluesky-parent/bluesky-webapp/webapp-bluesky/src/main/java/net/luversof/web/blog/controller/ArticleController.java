package net.luversof.web.blog.controller;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Article.Get;
import net.luversof.blog.domain.Article.Modify;
import net.luversof.blog.domain.Article.Save;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.BlueskyException;
import net.luversof.user.service.UserService;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.LuversofUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 소유한 Blog가 1개라는 가정으로 처리
 * @author bluesky
 *
 */
@Controller
//@RequestMapping("/blog/article")	// bluesky만 쓴다고 가정
@RequestMapping("/blog")
public class ArticleController {

	private static final int PAGE_BLOCK_SIZE = 10;
	
	@Autowired
	private BlogService blogService;

	@Autowired
	private ArticleService articleService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private ArticleCategoryService articleCategoryService;
	
	private Blog getBlog(Authentication authentication) {
		return blogService.findByUserId(((LuversofUser) authentication.getPrincipal()).getId()).get(0);
	}
	
	private Blog getBlog() {
		return blogService.findByUserId(userService.findByUsername("bluesky").getId()).get(0);
	}
	

	@RequestMapping
	public String list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Page<Article> articlePage = articleService.findByBlog(getBlog(), page - 1);
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
		return "/blog/list";
	}

	@RequestMapping("/{id}")
	public String view(@Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		return "/blog/view";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping("/write")
	public void writePage(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(articleCategoryService.findByBlog(getBlog(authentication)));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[blog].username == authentication.name")
	@RequestMapping("/{id}/modify")
	public String modifyPage(Authentication authentication, @Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		modelMap.addAttribute(articleCategoryService.findByBlog(blogService.findOne(article.getId())));
		return "/blog/modify";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public void save(Authentication authentication, @Validated(Save.class) Article article, ModelMap modelMap) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() != 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		article.setBlog(getBlog(authentication));
		modelMap.addAttribute("result", articleService.save(article).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public void modify(Authentication authentication, @Validated(Modify.class) Article article, ModelMap modelMap) {
		article.setBlog(getBlog(authentication));
		modelMap.addAttribute("result", articleService.update(article).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(Authentication authentication, @Validated(Get.class) Article article, ModelMap modelMap) {
		Article targetArticle = articleService.findOne(article.getId());
		if (((LuversofUser) authentication.getPrincipal()).getId() != targetArticle.getBlog().getUserId()) {
			throw new BlueskyException("username is not owner");
		}
		articleService.delete(article.getId());
	}
}
