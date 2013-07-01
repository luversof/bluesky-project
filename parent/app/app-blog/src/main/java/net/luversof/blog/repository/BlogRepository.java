package net.luversof.blog.repository;

import net.luversof.blog.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("blogRepository")
public interface BlogRepository extends JpaRepository<Blog, Long> {
}