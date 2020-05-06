package net.luversof.web.blog.controller;

import java.text.MessageFormat;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.blog.service.BlogService;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Controller
@RequestMapping(value = "/blog", produces = MediaType.TEXT_HTML_VALUE)
public class BlogViewController {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;
	
	@Autowired
	private BlogService blogService;

	/**
	 * 진입 페이지 blog 정보가 없는 경우 생성 페이지로 이동
	 * 
	 * @param blog
	 * @return
	 */
	@BlueskyPreAuthorize
	@GetMapping
	public String home() {
		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseGet(() -> {
			var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
			return blogService.createBlog(userId);
		});
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), userBlog.getId()));
	}

	@GetMapping(value = { "/{blogId}", "/{blogId}/view" })
	public String redirectArticleList(@PathVariable UUID blogId) {
		return String.join("", "redirect:", MessageFormat.format(messageSourceAccessor.getMessage("url.blog.view.list"), blogId));
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
	@GetMapping(value = "/{blogId}/view/{articleId}")
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
	@GetMapping(value = "/{blogId}/write")
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
	@GetMapping(value = "/{blogId}/modify/{articleId}")
	public String modifyPage(@PathVariable UUID blogId, @PathVariable long articleId) {
		return "blog/modify";
	}
}
