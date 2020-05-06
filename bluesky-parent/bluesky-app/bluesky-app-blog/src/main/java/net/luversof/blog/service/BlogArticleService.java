package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.repository.BlogArticleCategoryRepository;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

@Service
public class BlogArticleService {

	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogCommentService blogCommentService;
	
	public Page<BlogArticle> findByBlogId(UUID blogId, Pageable pageable) {
		return blogArticleRepository.findByBlogId(blogId, pageable);
	}
	
	public Optional<BlogArticle> findById(long id) {
		return blogArticleRepository.findById(id);
	}
	
	/**
	 * 조회수 증가 처리
	 * @param blogArticle
	 * @return
	 */
	public BlogArticle increaseViewCount(BlogArticle blogArticle) {
		blogArticle.setViewCount(blogArticle.getViewCount() + 1);
		return blogArticleRepository.save(blogArticle);
	}
	
	public BlogArticle create(BlogArticle blogArticle) {
		var userId = UserUtil.getLoginUser().orElseThrow(() -> new BlueskyException(UserErrorCode.NEED_LOGIN)).getId();
		var userBlog = blogService.findByUserId().get();
		blogArticle.setBlog(userBlog);
		blogArticle.setUserId(userId);
		
		checkBlogArtcieCategory(blogArticle);
		
		return blogArticleRepository.save(blogArticle);
	}
	
	public BlogArticle update(BlogArticle blogArticle) {
		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		var targetBlogArticle = blogArticleRepository.findById(blogArticle.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!targetBlogArticle.getBlog().getUserId().equals(userBlog.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		
		targetBlogArticle.setBlog(userBlog);
		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());
		
		checkBlogArtcieCategory(blogArticle);
		
		return blogArticleRepository.save(targetBlogArticle);
	}
	
	
	public void delete(long articleId) {
		var userBlog = blogService.findByUserId().get();
		var blogAarticle = blogArticleRepository.findById(articleId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!blogAarticle.getBlog().getUserId().equals(userBlog.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		blogArticleRepository.deleteById(articleId);
	}
	
	/**
	 * blogArticle에 blogArticleCategory가 있는 경우 해당 blogArticleCategory가 대상 유저의 blogArticleCategory인지 체크 후 entity 설정
	 * @param blogArticle
	 */
	private void checkBlogArtcieCategory(BlogArticle blogArticle) {
		if (blogArticle.getBlogArticleCategory() == null || blogArticle.getBlogArticleCategory().getId() <= 0) {
			blogArticle.setBlogArticleCategory(null);
			return;
		}
		
		var blogArticleCategory = blogArticleCategoryRepository.findById(blogArticle.getBlogArticleCategory().getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		if (!blogArticleCategory.getBlog().equals(blogArticle.getBlog())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLECATEGORY);
		}
		
		blogArticle.setBlogArticleCategory(blogArticleCategory);
		
	}
	
	public BlogArticle updateBlogCommentCount(long blogArticleId) {
		BlogArticle blogArticle = findById(blogArticleId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		long blogCommentCount = blogCommentService.countByBlogArticleId(blogArticleId);
		blogArticle.setBlogCommentCount(blogCommentCount);
		return blogArticleRepository.save(blogArticle);
	}
}
