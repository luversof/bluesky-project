package net.luversof.blog.repository.mysql;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.blog.domain.mysql.BlogArticleComment;

public interface BlogArticleCommentRepository extends JpaRepository<BlogArticleComment, Long> {
	
	Optional<BlogArticleComment> findByBlogArticleCommentId(String blogArticleCommentId);

	Page<BlogArticleComment> findByBlogArticleId(String blogArticleId, Pageable pageable);

	long countByBlogArticleId(String blogArticleId);
	
	void deleteByBlogArticleCommentId(String blogArticleCommentId);

}