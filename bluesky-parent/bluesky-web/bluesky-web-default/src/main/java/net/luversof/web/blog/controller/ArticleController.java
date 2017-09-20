//package net.luversof.web.blog.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.ui.ModelMap;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import net.luversof.blog.domain.Article;
//import net.luversof.blog.domain.Article.Get;
//import net.luversof.blog.domain.Article.Modify;
//import net.luversof.blog.domain.Article.Save;
//import net.luversof.blog.service.ArticleService;
//import net.luversof.web.constant.AuthorizeRole;
//
///**
// * @author bluesky
// *
// */
//@RestController
//@RequestMapping(value = "/blog/{blog.id}/article", produces = MediaType.APPLICATION_JSON_VALUE)
//public class ArticleController {
//
//	@Autowired
//	private ArticleService articleService;
//
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
//	@PostMapping
//	public Article save(@Validated(Save.class) Article article) {
//		return articleService.save(article);
//	}
//
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
//	@PutMapping(value = "/{id}")
//	public Article modify(@Validated(Modify.class) Article article) {
//		return articleService.update(article);
//	}
//
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
//	@DeleteMapping(value = "/{id}")
//	public boolean delete(@Validated(Get.class) Article article, ModelMap modelMap) {
//		articleService.delete(article.getId());
//		return true;
//	}
//}
