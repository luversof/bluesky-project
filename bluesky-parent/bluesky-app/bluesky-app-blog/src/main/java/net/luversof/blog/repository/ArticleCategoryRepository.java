package net.luversof.blog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;

@Transactional(readOnly = true)
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
	List<ArticleCategory> findByBlog(Blog blog);
}
