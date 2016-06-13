package net.luversof.blog.repository;

import static net.luversof.blog.BlogConstants.BLOG_TRANSACTIONMANAGER;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;

@Transactional(BLOG_TRANSACTIONMANAGER)
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
	
	@Transactional(value = BLOG_TRANSACTIONMANAGER, readOnly = true)
	List<ArticleCategory> findByBlog(Blog blog);
}
