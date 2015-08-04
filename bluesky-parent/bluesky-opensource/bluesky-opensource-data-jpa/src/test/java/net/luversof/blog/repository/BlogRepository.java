package net.luversof.blog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.blog.domain.Blog;

public interface BlogRepository extends JpaRepository<Blog, Long> {
	List<Blog> findByUserIdAndUserType(long userId, String userType); 
}
