package net.luversof.blog.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Article;

@Transactional(readOnly = true)
public interface ArticleRepository extends JpaRepository<Article, Long> {
	
	Page<Article> findByBlogId(@Param("id") UUID id, Pageable pageable);
	
}