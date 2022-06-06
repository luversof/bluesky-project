package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.repository.mysql.BlogArticleCategoryRepository;

@Service
public class BlogArticleCategoryService {

	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
//	public List<BlogArticleCategory> findByBlogId(UUID blogId) {
//		return blogArticleCategoryRepository.findByBlogId(blogId);
//	}

	public List<BlogArticleCategory> findByBlogId(String blogId) {
		return blogArticleCategoryRepository.findByBlogId(blogId);
	}
	
	public BlogArticleCategory create(BlogArticleCategory blogArticleCategory) {
		return blogArticleCategoryRepository.save(blogArticleCategory);
	}
	
	public BlogArticleCategory update(BlogArticleCategory blogArticleCategory) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findByBlogArticleCategoryId(blogArticleCategory.getBlogArticleCategoryId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		targetBlogArticleCategory.setName(blogArticleCategory.getName());
		return blogArticleCategoryRepository.save(targetBlogArticleCategory);
	}
	
	/**
	 * 해당 카테고리 글이 하나라도 있으면 삭제 불가 처리 (혹은 어딘가 다른 카테고리로 이동 처리하던가..)
	 * 굳이 그렇게 해야할까? 그냥 카테고리를 지우고 해당 카테고리 목록은 후처리를 고민하는게 더 나을거 같기도 한데?
	 */
	public void delete(long id) {
		var targetBlogArticleCategory = blogArticleCategoryRepository.findById(id).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		blogArticleCategoryRepository.delete(targetBlogArticleCategory);
	}
}
