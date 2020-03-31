package net.luversof.blog.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.BlogArticle;

@Transactional(readOnly = true)
public interface BlogArticleRepository extends JpaRepository<BlogArticle, Long> {
	
	Page<BlogArticle> findByBlogId(UUID blogId, Pageable pageable);
	
}