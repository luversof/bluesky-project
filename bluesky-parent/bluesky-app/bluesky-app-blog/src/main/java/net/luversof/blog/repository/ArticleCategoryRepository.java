package net.luversof.blog.repository;

import java.util.List;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
	List<ArticleCategory> findByBlog(Blog blog);
}
