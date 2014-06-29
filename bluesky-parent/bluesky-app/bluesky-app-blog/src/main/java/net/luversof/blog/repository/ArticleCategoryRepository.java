package net.luversof.blog.repository;

import java.util.List;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long>, QueryDslPredicateExecutor<ArticleCategory> {
	List<ArticleCategory> findByBlog(Blog blog);
}
