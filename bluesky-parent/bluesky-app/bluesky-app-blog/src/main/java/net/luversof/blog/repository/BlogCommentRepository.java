package net.luversof.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.BlogComment;

@Transactional(readOnly = true)
public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

	Page<BlogComment> findByBlogArticleId(long blogArticleId, Pageable pageable);
	
}
