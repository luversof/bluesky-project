package net.luversof.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class BlogArticleService {

	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogService blogService;
	
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
		Blog userBlog = blogService.findByUserId().get();
		blogArticle.setBlog(userBlog);
		return blogArticleRepository.save(blogArticle);
	}
	
	public BlogArticle update(BlogArticle blogArticle) {
		
		Blog userBlog = blogService.findByUserId().get();
		
		BlogArticle targetBlogArticle = blogArticleRepository.findById(blogArticle.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!targetBlogArticle.getBlog().getUserId().equals(userBlog.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		
		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());
		
		return blogArticleRepository.save(targetBlogArticle);
	}
	
	
	public void delete(Long articleId) {
		Blog userBlog = blogService.findByUserId().get();
		BlogArticle blogAarticle = blogArticleRepository.findById(articleId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!blogAarticle.getBlog().getUserId().equals(userBlog.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		blogArticleRepository.deleteById(articleId);
	}
}
