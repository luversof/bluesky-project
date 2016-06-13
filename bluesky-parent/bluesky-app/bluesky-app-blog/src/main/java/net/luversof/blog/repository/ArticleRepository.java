package net.luversof.blog.repository;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;

import static net.luversof.blog.BlogConstants.BLOG_TRANSACTIONMANAGER;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(BLOG_TRANSACTIONMANAGER)
public interface ArticleRepository extends JpaRepository<Article, Long> {
	
	@Transactional(value = BLOG_TRANSACTIONMANAGER, readOnly = true)
	Page<Article> findByBlog(Blog blog, Pageable pageable);
}