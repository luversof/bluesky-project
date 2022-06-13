package net.luversof.blog.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.repository.mysql.BlogArticleCategoryRepository;

@Service
public class BlogArticleCategoryService {

	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	public List<BlogArticleCategory> findByBlogId(String blogId) {
		return blogArticleCategoryRepository.findByBlogId(blogId);
	}
	
	public BlogArticleCategory create(@BlueskyValidated(BlogArticleCategory.Create.class) BlogArticleCategory blogArticleCategory) {
		blogArticleCategory.setBlogArticleCategoryId(UUID.randomUUID().toString());
		return blogArticleCategoryRepository.save(blogArticleCategory);
	}
	
	public BlogArticleCategory update(@BlueskyValidated(BlogArticleCategory.Update.class) BlogArticleCategory blogArticleCategory) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticleCategory.getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		targetBlogArticleCategory.setName(blogArticleCategory.getName());
		return blogArticleCategoryRepository.save(targetBlogArticleCategory);
	}
	
	/**
	 * 해당 카테고리 글이 하나라도 있으면 삭제 불가 처리 해야 하지 않을까?
	 */
	public void delete(@BlueskyValidated(BlogArticleCategory.Delete.class) BlogArticleCategory blogArticleCategory) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticleCategory.getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		blogArticleCategoryRepository.delete(targetBlogArticleCategory);
	}
}
