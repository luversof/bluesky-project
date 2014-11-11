package net.luversof.blog.repository;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
	Page<Article> findByBlog(Blog blog, Pageable pageable);
}