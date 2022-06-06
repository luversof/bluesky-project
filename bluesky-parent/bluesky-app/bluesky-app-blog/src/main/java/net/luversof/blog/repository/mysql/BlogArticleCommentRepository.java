package net.luversof.blog.repository.mysql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.BlogArticleComment;

@Transactional(readOnly = true)
public interface BlogArticleCommentRepository extends JpaRepository<BlogArticleComment, Long> {

	Page<BlogArticleComment> findByBlogArticleId(long blogArticleId, Pageable pageable);

	long countByBlogArticleId(long blogArticleId);
	
}
