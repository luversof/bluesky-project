package net.luversof.web.blog.controller;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Article.Get;
import net.luversof.blog.domain.Article.Modify;
import net.luversof.blog.domain.Article.Save;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;
import net.luversof.core.BlueskyException;
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

@Controller
@RequestMapping("/blog")
public class BlogController {

	private static final int PAGE_BLOCK_SIZE = 10;
	
	@Autowired
	private BlogService blogService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ArticleCategoryService articleCategoryService;
	

	@RequestMapping("")
	public String list(@RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
		Page<Article> blogPage = articleService.findAll(page - 1);
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
	public String view(@Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		return "/blog/view";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping("/write")
	public void writePage(Article article, Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(articleCategoryService.findByBlog(article.getBlog()));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[blog].username == authentication.name")
	@RequestMapping("/{id}/modify")
	public String modifyPage(@Validated(Get.class) Article article, Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		modelMap.addAttribute(articleCategoryService.findByBlog(blogService.findOne(article.getId())));
		return "/blog/modify";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public void save(@Validated(Save.class) Article article, Authentication authentication, ModelMap modelMap) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() != 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		modelMap.addAttribute("result", articleService.save(article).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public void modify(@Validated(Modify.class) Article blog, Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute("result", articleService.update(blog).getId());
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(@Validated(Get.class) Article article, Authentication authentication, ModelMap modelMap) {
		Article targetArticle = articleService.findOne(article.getId());
		if (((LuversofUser) authentication.getPrincipal()).getId() != targetArticle.getBlog().getUserId()) {
			throw new BlueskyException("username is not owner");
		}
		articleService.delete(article.getId());
	}
}
