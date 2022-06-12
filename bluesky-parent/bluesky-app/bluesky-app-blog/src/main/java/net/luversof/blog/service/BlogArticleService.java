package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticle;
import net.luversof.blog.repository.mysql.BlogArticleCategoryRepository;
import net.luversof.blog.repository.mysql.BlogArticleRepository;

@Service
public class BlogArticleService {

	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	@Autowired
	private BlogService blogService;
	
//	public Optional<BlogArticle> findById(long id) {
//		return blogArticleRepository.findById(id);
//	}
	
	public Page<BlogArticle> findByBlogId(String blogId, Pageable pageable) {
		return blogArticleRepository.findByBlogId(blogId, pageable);
	}
	
	public Optional<BlogArticle> findByBlogArticleId(String blogArticleId) {
		return blogArticleRepository.findByBlogArticleId(blogArticleId);
	}
	
	
	public BlogArticle create(@BlueskyValidated(BlogArticle.Create.class) BlogArticle blogArticle) {
		// 존재하는 blog인지 확인
		blogService.findByBlogId(blogArticle.getBlogId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		blogArticle.setBlogArticleId(UUID.randomUUID().toString());
		
		checkBlogArtcieCategory(blogArticle);
		
		return blogArticleRepository.save(blogArticle);
	}
	
	public BlogArticle update(BlogArticle blogArticle) {
		var targetBlogArticle = blogArticleRepository.findByBlogArticleId(blogArticle.getBlogArticleId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!targetBlogArticle.getUserId().equals(blogArticle.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		
		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());
		
		checkBlogArtcieCategory(blogArticle);
		
		return blogArticleRepository.save(targetBlogArticle);
	}
	
	
	public void deleteByBlogArticleId(String blogArticleId) {
//		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
//		var blogAarticle = blogArticleRepository.findById(blogArticleId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
//		if (!blogAarticle.getBlog().getUserId().equals(userBlog.getUserId())) {
//			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
//		}
		blogArticleRepository.deleteByBlogArticleId(blogArticleId);
	}
	
	/**
	 * blogArticle에 blogArticleCategory가 있는 경우 해당 blogArticleCategory가 대상 유저의 blogArticleCategory인지 체크 후 entity 설정
	 * @param blogArticle
	 */
	private void checkBlogArtcieCategory(BlogArticle blogArticle) {
		if (blogArticle.getBlogArticleCategory() == null || blogArticle.getBlogArticleCategory().getIdx() <= 0) {
//			blogArticle.setBlogArticleCategory(null);
			return;
		}
		
		var blogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticle.getBlogArticleCategory().getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		if (!blogArticleCategory.getBlogId().equals(blogArticle.getBlogId())) {
			throw new BlueskyException(BlogErrorCode.NOT_TARGET_BLOGARTICLECATEGORY);
		}
		
		blogArticle.setBlogArticleCategory(blogArticleCategory);
		
	}
}
