package net.luversof.blog.service;

import java.util.Optional;

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
public class BlogCommentService {

	@Autowired
	private BlogArticleCommentRepository blogCommentRepository;
	
	public Page<BlogArticleComment> findByBlogArticleId(long blogArticleId, Pageable pageable) {
		Page<BlogArticleComment> blogCommentPage = blogCommentRepository.findByBlogArticleId(blogArticleId, pageable);
		return blogCommentPage;
	}
	
	public Optional<BlogArticleComment> findById(long blogCommentId) {
		Optional<BlogArticleComment> BlogCommentOptional = blogCommentRepository.findById(blogCommentId);
		return BlogCommentOptional;
	}
	
	public BlogArticleComment create(BlogArticleComment blogComment) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getUserId();
		if (!StringUtils.hasText(blogComment.getBlogArticleId())) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOGARTICLE_ID);
		}
		
		BlogRequestAttributeUtil.getBlogArticleService().findByBlogArticleId(blogComment.getBlogArticleId());
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
	
	public void delete(long commentId) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getUserId();
		var blogComment = blogCommentRepository.findById(commentId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!blogComment.getUserId().equals(userId)) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		
		blogCommentRepository.deleteById(commentId);
	}
	
	public long countByBlogArticleId(long blogArticleId) {
		return blogCommentRepository.countByBlogArticleId(blogArticleId);
	}
	
}
