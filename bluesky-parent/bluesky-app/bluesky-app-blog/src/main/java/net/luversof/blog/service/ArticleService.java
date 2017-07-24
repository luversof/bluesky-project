package net.luversof.blog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.exception.BlogErrorCode;
import net.luversof.blog.domain.Article;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.core.exception.BlueskyException;

@Service
public class ArticleService {

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private CategoryService categoryService;
	
	public Article save(Article blogArticle) {
		if (blogArticle.getCategory() != null && blogArticle.getCategory().getId() > 0) {
			blogArticle.setCategory(categoryService.findOne(blogArticle.getCategory().getId()));
		}
		return articleRepository.save(blogArticle);
	}
	
	public Article update(Article blogArticle) {
		Article targetBlogArticle = findById(blogArticle.getId()).get();
		if (!blogArticle.getBlog().equals(targetBlogArticle.getBlog())) {
			throw new BlueskyException(BlogErrorCode.NOT_USER_BLOG);
		}
		targetBlogArticle.setTitle(blogArticle.getTitle());
		targetBlogArticle.setContent(blogArticle.getContent());
		if (blogArticle.getCategory() != null && blogArticle.getCategory().getId() != 0) {
			targetBlogArticle.setCategory(categoryService.findOne(blogArticle.getCategory().getId()));
		}
		return articleRepository.save(targetBlogArticle);
	}

	public Optional<Article> findById(long id) {
		return articleRepository.findById(id);
	}
	
	public void incraseViewCount(Article blogArticle) {
		blogArticle.setViewCount(blogArticle.getViewCount() + 1);
		articleRepository.save(blogArticle);
	}

	public Page<Article> findByBlog(Blog blog, Pageable pageable) {
		return articleRepository.findByBlog(blog, pageable);
	}

	public void delete(long id) {
		articleRepository.deleteById(id);
	}
}
