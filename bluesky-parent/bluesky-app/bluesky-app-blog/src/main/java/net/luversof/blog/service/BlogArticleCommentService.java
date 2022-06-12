package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticleComment;
import net.luversof.blog.repository.mysql.BlogArticleCommentRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Service
public class BlogArticleCommentService {

	@Autowired
	private BlogArticleCommentRepository blogCommentRepository;
	
	public Page<BlogArticleComment> findByBlogArticleId(String blogArticleId, Pageable pageable) {
		Page<BlogArticleComment> blogCommentPage = blogCommentRepository.findByBlogArticleId(blogArticleId, pageable);
		return blogCommentPage;
	}
	
	public Optional<BlogArticleComment> findByBlogArticleCommentId(String blogArticleCommentId) {
		Optional<BlogArticleComment> BlogCommentOptional = blogCommentRepository.findByBlogArticleCommentId(blogArticleCommentId);
		return BlogCommentOptional;
	}
	
	public BlogArticleComment create(BlogArticleComment blogComment) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getUserId();
		if (!StringUtils.hasText(blogComment.getBlogArticleId())) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOGARTICLE_ID);
		}
		
		BlogRequestAttributeUtil.getBlogArticleService().findByBlogArticleId(blogComment.getBlogArticleId());
		blogComment.setBlogArticleCommentId(UUID.randomUUID().toString());
		blogComment.setUserId(userId);
		
		return blogCommentRepository.save(blogComment);
	}
	
	public BlogArticleComment update(BlogArticleComment blogComment) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getUserId();
		var targetBlogComment = blogCommentRepository.findById(blogComment.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!targetBlogComment.getUserId().equals(userId)) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		
		targetBlogComment.setComment(blogComment.getComment());
		
		return blogCommentRepository.save(targetBlogComment);
	}
	
	public void deleteByBlogArticleCommentId(String blogArticleCommentId) {
		blogCommentRepository.deleteByBlogArticleCommentId(blogArticleCommentId);
	}
	
	public long countByBlogArticleId(String blogArticleId) {
		return blogCommentRepository.countByBlogArticleId(blogArticleId);
	}
	
}
