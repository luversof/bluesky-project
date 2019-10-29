package net.luversof.web.blog.controller;

import java.text.MessageFormat;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;


@Controller
@RequestMapping(value = "/blog/{blogId}", produces = MediaType.TEXT_HTML_VALUE)
public class ArticleViewController {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;

	@GetMapping(value = { "", "/view" })
	public String redirectArticleList(@PathVariable UUID blogId) {
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), blogId));
	}

	@GetMapping(value = "/create")
	public void createForm() {}

	/**
	 * 글 목록
	 * 
	 * @param blogId
	 * @param page
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/list")
	public String list(@PathVariable UUID blogId) {
		return "blog/list";
	}

	/**
	 * 글 보기
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@GetMapping(value = "/view/{articleId}")
	public String view(@PathVariable UUID blogId, @PathVariable long articleId) {
		return "blog/view";
	}

	/**
	 * 글 쓰기
	 * 
	 * @param blog
	 * @param modelMap
	 * @return
	 */
	@BlueskyPreAuthorize
	@GetMapping(value = "/write")
	public String writePage(@PathVariable UUID blogId) {
		return "blog/write";
	}

	/**
	 * 글 수정
	 * 
	 * @param article
	 * @param modelMap
	 * @return
	 */
	@BlueskyPreAuthorize
	@GetMapping(value = "/modify/{articleId}")
	public String modifyPage(@PathVariable UUID blogId, @PathVariable long articleId) {
		return "blog/modify";
	}
}
