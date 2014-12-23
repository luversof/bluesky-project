package net.luversof.opensource.data.jpa.repository;

import java.util.List;

import net.luversof.opensource.data.jpa.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {
	List<Blog> findByUserIdAndUserType(long userId, String userType); 
}
