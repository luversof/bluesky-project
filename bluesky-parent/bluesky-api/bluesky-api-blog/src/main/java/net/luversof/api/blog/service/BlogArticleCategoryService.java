package net.luversof.api.blog.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.blog.constant.BlogErrorCode;
import net.luversof.api.blog.domain.mariadb.BlogArticleCategory;
import net.luversof.api.blog.repository.mariadb.BlogArticleCategoryRepository;

@Service
public class BlogArticleCategoryService {

	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	public BlogArticleCategory create(BlogArticleCategory blogArticleCategory) {
		blogArticleCategory.setBlogArticleCategoryId(UUID.randomUUID().toString());
		return blogArticleCategoryRepository.save(blogArticleCategory);
	}
	
	public List<BlogArticleCategory> findByBlogId(String blogId) {
		return blogArticleCategoryRepository.findByBlogId(blogId);
	}
	
	public BlogArticleCategory update(BlogArticleCategory blogArticleCategory) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticleCategory.getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		targetBlogArticleCategory.setName(blogArticleCategory.getName());
		return blogArticleCategoryRepository.save(targetBlogArticleCategory);
	}
	
	/**
	 * 해당 카테고리 글이 하나라도 있으면 삭제 불가 처리 해야 하지 않을까?
	 */
	public void delete(BlogArticleCategory blogArticleCategory) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticleCategory.getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		blogArticleCategoryRepository.delete(targetBlogArticleCategory);
	}
}
