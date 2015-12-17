package net.luversof.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.blog.domain.Blog;

public interface BlogRepository extends JpaRepository<Blog, Long> {
	Blog findByUserId(long userId); 
}
