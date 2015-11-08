package net.luversof.web.blog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Article.Get;
import net.luversof.blog.domain.Article.Modify;
import net.luversof.blog.domain.Article.Save;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.web.blog.annotation.CheckBlogAndAddToArticle;
import net.luversof.web.constant.AuthorizeRole;

/**
 * @author bluesky
 *
 */
@RestController
@RequestMapping("blog")
public class ArticleController {


	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleService articleService;


	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Article save(@CheckBlogAndAddToArticle @Validated(Save.class) Article article) {
		if (article.getArticleCategory() != null && article.getArticleCategory().getId() != 0) {
			article.setArticleCategory(articleCategoryService.findOne(article.getArticleCategory().getId()));
		}
		return articleService.save(article);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public Article modify(/*@CheckBlogAndAddToArticle @Validated(Modify.class)*/ Article article) {
		return articleService.update(article);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{blog.id}/article/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean delete(@CheckBlogAndAddToArticle @Validated(Get.class) Article article, ModelMap modelMap) {
		articleService.delete(article.getId());
		return true;
	}
}
