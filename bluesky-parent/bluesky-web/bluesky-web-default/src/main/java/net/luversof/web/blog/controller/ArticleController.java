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
import net.luversof.web.blog.annotation.CheckBlog;
import net.luversof.web.blog.annotation.CheckBlogAndAddToArticle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author bluesky
 *
 */
@Controller
@RequestMapping("blog")
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

	/**
	 * 글목록
	 * 
	 * @param blogId
	 * @param page
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = "/{blogId}/article", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String list(@PathVariable long blogId, @RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
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
	 * 글보기
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value = "/{blog.id}/article/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String view(@Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		return "blog/article/view";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}/article/write", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String writePage(@CheckBlog Blog blog, ModelMap modelMap) {
		modelMap.addAttribute(articleCategoryService.findByBlog(blog));
		return "blog/article/write";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("hasRole('ROLE_USER') && #modelMap[article].blog.userId == authentication.principal.id")
	@RequestMapping(value = "/{blog.id}/article/{id}/modify", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String modifyPage(@CheckBlogAndAddToArticle @Validated(Get.class) Article article, ModelMap modelMap) {
		modelMap.addAttribute(articleService.findOne(article.getId()));
		modelMap.addAttribute(articleCategoryService.findByBlog(blogService.findOne(article.getId())));
		return "blog/article/modify";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Article save(@CheckBlogAndAddToArticle @Validated(Save.class) Article article) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() != 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		return articleService.save(article);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Article modify(@CheckBlogAndAddToArticle @Validated(Modify.class) Article article) {
		return articleService.update(article);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(@CheckBlogAndAddToArticle @Validated(Get.class) Article article, ModelMap modelMap) {
		articleService.delete(article.getId());
	}
}