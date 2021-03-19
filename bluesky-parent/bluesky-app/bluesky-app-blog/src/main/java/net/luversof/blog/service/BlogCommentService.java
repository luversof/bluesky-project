package net.luversof.blog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogComment;
import net.luversof.blog.repository.mysql.BlogCommentRepository;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;
import net.luversof.user.util.UserUtil;

@Service
public class BlogCommentService {

	@Autowired
	private BlogCommentRepository blogCommentRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private BlogArticleService blogArticleService;

	public Page<BlogComment> findByBlogArticleId(long blogArticleId, Pageable pageable) {
		Page<BlogComment> blogCommentPage = blogCommentRepository.findByBlogArticleId(blogArticleId, pageable);
		setUserName(blogCommentPage);
		return blogCommentPage;
	}
	
	public Optional<BlogComment> findById(long blogCommentId) {
		Optional<BlogComment> BlogCommentOptional = blogCommentRepository.findById(blogCommentId);
		if (BlogCommentOptional.isPresent()) {
			setUserName(BlogCommentOptional.get());
		}
		return BlogCommentOptional;
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
	
	public long countByBlogArticleId(long blogArticleId) {
		return blogCommentRepository.countByBlogArticleId(blogArticleId);
	}
	
	
	private void setUserName(Iterable<BlogComment> blogCommentIterable) {
		List<UUID> ids = new ArrayList<>();
		blogCommentIterable.forEach(blogComment -> ids.add(blogComment.getUserId()));
		
		List<User> userList = userService.findByIdIn(ids);
		userList.forEach(user -> {
			blogCommentIterable.forEach(blogComment -> {
				if (blogComment.getUserId().equals(user.getId())) {
					blogComment.setUser(user);
				}
			});
		});
	}
	
	private void setUserName(BlogComment blogComment) {
		User user = userService.findById(blogComment.getUserId()).orElse(null);
		blogComment.setUser(user);
	}
}
