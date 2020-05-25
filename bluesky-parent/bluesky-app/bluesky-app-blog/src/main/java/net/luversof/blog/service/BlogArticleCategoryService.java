package net.luversof.blog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.constant.BlogErrorCode;
import net.luversof.blog.domain.BlogArticleCategory;
import net.luversof.blog.repository.BlogArticleCategoryRepository;
import net.luversof.blog.util.BlogRequestAttributeUtil;
import net.luversof.boot.exception.BlueskyException;
import net.luversof.user.constant.UserErrorCode;
import net.luversof.user.util.UserUtil;

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
		var userBlog = blogService.findByUserId().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_USER_BLOG));
		return blogArticleCategoryRepository.findByBlog(userBlog);
	}
	
	public BlogArticleCategory create(BlogArticleCategory blogArticleCategory) {
		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		
		blogArticleCategory.setBlog(userBlog);
		
		return blogArticleCategoryRepository.save(blogArticleCategory);
	}
	
	public BlogArticleCategory update(BlogArticleCategory blogArticleCategory) {
		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
		var targetBlogArticleCategory = blogArticleCategoryRepository.findById(blogArticleCategory.getId()).orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY));
		if (!targetBlogArticleCategory.getBlog().getUserId().equals(userBlog.getUserId())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOGARTICLECATEGORY);
		}
		
		targetBlogArticleCategory.setName(blogArticleCategory.getName());
		return blogArticleCategoryRepository.save(targetBlogArticleCategory);
	}
	
	/**
	 * 해당 카테고리 글이 하나라도 있으면 삭제 불가 처리 (혹은 어딘가 다른 카테고리로 이동 처리하던가..)
	 * 굳이 그렇게 해야할까? 그냥 카테고리를 지우고 해당 카테고리 목록은 후처리를 고민하는게 더 나을거 같기도 한데?
	 */
	public void delete(long blogArticleCategoryId) {
		var userBlog = BlogRequestAttributeUtil.getUserBlog().orElseThrow(() -> new BlueskyException(BlogErrorCode.NOT_EXIST_BLOG));
	}
}
