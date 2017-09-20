package net.luversof.web.blog.controller;

import java.text.MessageFormat;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Article.Get;
import net.luversof.blog.domain.Blog;
import net.luversof.core.exception.BlueskyException;
import net.luversof.web.constant.AuthorizeRole;

@Controller
@RequestMapping(value = "/blog/{blog.id}", produces = MediaType.TEXT_HTML_VALUE)
public class ArticleViewController {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;

//	@Autowired
//	private BlogService blogService;
//
//	@Autowired
//	private ArticleService articleService;
//
//	@Autowired
//	private CategoryService categoryService;

	@GetMapping(value = { "", "/view" })
	public String redirectArticleList(@PathVariable(name = "blog.id") UUID blogId) {
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), blogId));
	}

	@GetMapping(value = "/create")
	public void createForm() {
	}

	/**
	 * 글 목록
	 * 
	 * @param blogId
	 * @param page
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/list")
	public String list(@PathVariable(name = "blog.id") UUID blogId, @PageableDefault(sort = { "id" }, direction = Direction.DESC) Pageable pageable, ModelMap modelMap) {
//		Blog blog = blogService.findById(blogId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
//		Page<Article> articlePage = articleService.findByBlog(blog, pageable);
//		modelMap.addAttribute("articlePage", articlePage);
//		modelMap.addAttribute("blogId", blogId);
		return "blog/list";
	}

	/**
	 * 글 보기
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/view/{id}")
	public String view(@Validated(Get.class) Article article, ModelMap modelMap) {
//		Article viewArticle = articleService.findById(article.getId()).get();
//		articleService.incraseViewCount(viewArticle);
//		modelMap.addAttribute("article", viewArticle);
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
	@GetMapping(value = "/write")
	public String writePage(@PathVariable(name = "blog.id") UUID blogId, ModelMap modelMap) {
//		modelMap.addAttribute("categoryList", categoryService.findByBlogId(blogId));
//		modelMap.addAttribute("blogId", blogId);
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
	@GetMapping(value = "/modify/{id}")
	public String modifyPage(@Validated(Get.class) Article article, ModelMap modelMap) {
//		modelMap.addAttribute("article", articleService.findById(article.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_ARTICLE)));
//		modelMap.addAttribute("categoryList", categoryService.findByBlog(blogService.findById(article.getBlog().getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG))));
		return "blog/modify";
	}
}
