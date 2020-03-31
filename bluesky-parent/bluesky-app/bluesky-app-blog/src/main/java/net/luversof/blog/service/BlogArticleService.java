package net.luversof.blog.service;

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
	
	public BlogArticle save(BlogArticle blogArticle) {
		Blog userBlog = blogService.findByUserId().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_USER_BLOG));
		blogArticle.setBlog(userBlog);
		return blogArticleRepository.save(blogArticle);
	}
	
	
	public void delete(UUID userId, Long articleId) {
		BlogArticle blogAarticle = blogArticleRepository.findById(articleId).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLE));
		if (!blogAarticle.getBlog().getUserId().equals(userId)) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLE);
		}
		blogArticleRepository.deleteById(articleId);
	}
}
