package net.luversof.blog.repository;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, QueryDslPredicateExecutor<Article> {
	Page<Article> findByBlog(Blog blog, Pageable pageable);
}