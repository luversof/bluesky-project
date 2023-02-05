package net.luversof.api.blog.repository.mariadb;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.blog.domain.mariadb.BlogArticle;

public interface BlogArticleRepository extends JpaRepository<BlogArticle, Long> {
	
	Optional<BlogArticle> findByBlogArticleId(String blogArticleId);
	
	Page<BlogArticle> findByBlogId(String blogId, Pageable pageable);
	
	void deleteByBlogArticleId(String blogArticleId);
	
}