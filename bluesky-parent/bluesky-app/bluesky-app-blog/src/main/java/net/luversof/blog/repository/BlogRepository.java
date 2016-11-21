package net.luversof.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;

@Transactional(readOnly = true)
public interface BlogRepository extends JpaRepository<Blog, Long> {
	
	Blog findByUserId(String userId); 
}
