package net.luversof.blog.repository;

import java.util.List;

import net.luversof.blog.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {
	List<Blog> findByUserId(long userId); 
}
