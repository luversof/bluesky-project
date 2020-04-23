package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticleCategory;
import net.luversof.blog.repository.BlogArticleCategoryRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class BlogArticleCategoryService {

	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	@Autowired
	private BlogService blogService;
	
//	public List<BlogArticleCategory> findByBlogId(UUID blogId) {
//		return blogArticleCategoryRepository.findByBlogId(blogId);
//	}

	/**
	 * 로그인한 유저의 BlogArticleCategoryList 조회
	 * @return
	 */
	public List<BlogArticleCategory> findByUserBlogId() {
		Blog userBlog = blogService.findByUserId().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_BLOG));
		return blogArticleCategoryRepository.findByBlog(userBlog);
	}
}
