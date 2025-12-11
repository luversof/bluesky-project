package net.luversof.api.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.api.blog.constant.BlogErrorCode;
import net.luversof.api.blog.domain.mariadb.BlogArticle;
import net.luversof.api.blog.repository.mariadb.BlogArticleCategoryRepository;
import net.luversof.api.blog.repository.mariadb.BlogArticleRepository;

@Service
public class BlogArticleService {

	@Autowired
	private BlogArticleRepository blogArticleRepository;

	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;

	@Autowired
	private BlogService blogService;

	public BlogArticle create(BlogArticle blogArticle) {
		// 존재하는 blog인지 확인
		blogService.findByBlogId(blogArticle.getBlogId()).orElseThrow(BlogErrorCode.NOT_EXIST_BLOG::exception);
		blogArticle.setBlogArticleId(UUID.randomUUID().toString());

		checkBlogArtcieCategory(blogArticle);

		return blogArticleRepository.save(blogArticle);
	}

	public Page<BlogArticle> findByBlogId(String blogId, Pageable pageable) {
		return blogArticleRepository.findByBlogId(blogId, pageable);
	}

	public Optional<BlogArticle> findByBlogArticleId(String blogArticleId) {
		return blogArticleRepository.findByBlogArticleId(blogArticleId);
	}

	public BlogArticle update(BlogArticle blogArticle) {
		var targetBlogArticle = blogArticleRepository.findByBlogArticleId(blogArticle.getBlogArticleId())
				.orElseThrow(BlogErrorCode.NOT_EXIST_BLOGARTICLE::exception);
		if (!targetBlogArticle.getUserId().equals(blogArticle.getUserId())) {
			BlogErrorCode.NOT_USER_BLOGARTICLE.throwException();
		}

		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());

		checkBlogArtcieCategory(blogArticle);

		return blogArticleRepository.save(targetBlogArticle);
	}

	public void delete(BlogArticle blogArticle) {
		var targetBlogArticle = blogArticleRepository.findByBlogArticleId(blogArticle.getBlogArticleId())
				.orElseThrow(BlogErrorCode.NOT_EXIST_BLOGARTICLE::exception);
		if (!targetBlogArticle.getUserId().equals(blogArticle.getUserId())) {
			BlogErrorCode.NOT_USER_BLOGARTICLE.throwException();
			;
		}
		blogArticleRepository.delete(targetBlogArticle);
	}

	/**
	 * blogArticle에 blogArticleCategory가 있는 경우 해당 blogArticleCategory가 대상 유저의
	 * blogArticleCategory인지 체크 후 entity 설정
	 * 
	 * @param blogArticle
	 */
	private void checkBlogArtcieCategory(BlogArticle blogArticle) {
		if (blogArticle.getBlogArticleCategory() == null || blogArticle.getBlogArticleCategory().getIdx() <= 0) {
			return;
		}

		var blogArticleCategory = blogArticleCategoryRepository
				.findByBlogArticleCategoryId(blogArticle.getBlogArticleCategory().getBlogArticleCategoryId())
				.orElseThrow(BlogErrorCode.NOT_EXIST_BLOGARTICLECATEGORY::exception);
		if (!blogArticleCategory.getBlogId().equals(blogArticle.getBlogId())) {
			BlogErrorCode.NOT_TARGET_BLOGARTICLECATEGORY.throwException();
		}

		blogArticle.setBlogArticleCategory(blogArticleCategory);

	}
}
