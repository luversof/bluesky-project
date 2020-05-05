package net.luversof.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.BlogComment;
import net.luversof.blog.repository.BlogCommentRepository;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Service
public class BlogCommentService {

	@Autowired
	private BlogCommentRepository blogCommentRepository;
	
	@Autowired
	private BlogArticleService blogArticleService;

	public Page<BlogComment> findByBlogArticleId(long blogArticleId, Pageable pageable) {
		return blogCommentRepository.findByBlogArticleId(blogArticleId, pageable);
	}
	
	public BlogComment create(BlogComment blogComment) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
		if (blogComment.getBlogArticle() == null || blogComment.getBlogArticle().getId() <= 0) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOGARTICLE_ID);
		}
		
		blogArticleService.findById(blogComment.getBlogArticle().getId());
		blogComment.setUserId(userId);
		
		return blogCommentRepository.save(blogComment);
	}
	
	public BlogComment update(BlogComment blogComment) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
		var targetBlogComment = blogCommentRepository.findById(blogComment.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!targetBlogComment.getUserId().equals(userId)) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		
		targetBlogComment.setComment(blogComment.getComment());
		
		return blogCommentRepository.save(targetBlogComment);
	}
	
	public void delete(long commentId) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
		var blogComment = blogCommentRepository.findById(commentId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!blogComment.getUserId().equals(userId)) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		
		blogCommentRepository.deleteById(commentId);
	}
}
