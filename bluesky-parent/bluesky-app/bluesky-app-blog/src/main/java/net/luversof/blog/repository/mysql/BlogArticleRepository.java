package net.luversof.blog.repository.mysql;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.BlogArticle;

@Transactional(readOnly = true)
public interface BlogArticleRepository extends JpaRepository<BlogArticle, Long> {
	
	Optional<BlogArticle> findByBlogArticleId(String blogArticleId);
	
	Page<BlogArticle> findByBlogId(String blogId, Pageable pageable);
	
}