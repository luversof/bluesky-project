package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticleComment;
import net.luversof.blog.repository.mysql.BlogArticleCommentRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;

@Service
public class BlogArticleCommentService {

	@Autowired
	private BlogArticleCommentRepository blogCommentRepository;
	
	public Page<BlogArticleComment> findByBlogArticleId(String blogArticleId, Pageable pageable) {
		return blogCommentRepository.findByBlogArticleId(blogArticleId, pageable);
	}
	
	public Optional<BlogArticleComment> findByBlogArticleCommentId(String blogArticleCommentId) {
		return blogCommentRepository.findByBlogArticleCommentId(blogArticleCommentId);
	}
	
	public BlogArticleComment create(@BlueskyValidated(BlogArticleComment.Create.class) BlogArticleComment blogArticleComment) {
		if (!StringUtils.hasText(blogArticleComment.getBlogArticleId())) {
			throw new BlueskyException(BlogErrorCode.NOT_EXIST_PARAMETER_BLOGARTICLE_ID);
		}
		
		BlogRequestAttributeUtil.getBlogArticleService().findByBlogArticleId(blogArticleComment.getBlogArticleId());
		blogArticleComment.setBlogArticleCommentId(UUID.randomUUID().toString());
		
		return blogCommentRepository.save(blogArticleComment);
	}
	
	public BlogArticleComment update(@BlueskyValidated(BlogArticleComment.Update.class) BlogArticleComment blogArticleComment) {
		var targetBlogComment = blogCommentRepository.findByBlogArticleCommentId(blogArticleComment.getBlogArticleCommentId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGCOMMENT));
		if (!targetBlogComment.getUserId().equals(blogArticleComment.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGCOMMENT);
		}
		
		targetBlogComment.setComment(blogArticleComment.getComment());
		
		return blogCommentRepository.save(targetBlogComment);
	}
	
	public void delete(@BlueskyValidated(BlogArticleComment.Delete.class) BlogArticleComment blogArticleComment) {
		blogCommentRepository.deleteByBlogArticleCommentId(blogArticleComment.getBlogArticleCommentId());
	}
	
	public long countByBlogArticleId(String blogArticleId) {
		return blogCommentRepository.countByBlogArticleId(blogArticleId);
	}
	
}
