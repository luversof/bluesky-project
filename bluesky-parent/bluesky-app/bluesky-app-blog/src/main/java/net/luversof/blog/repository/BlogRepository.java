package net.luversof.blog.repository;

import static net.luversof.blog.BlogConstants.BLOG_TRANSACTIONMANAGER;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;

@Transactional(BLOG_TRANSACTIONMANAGER)
public interface BlogRepository extends JpaRepository<Blog, Long> {
	
	@Transactional(value = BLOG_TRANSACTIONMANAGER, readOnly = true)
	Blog findByUserId(long userId); 
}
