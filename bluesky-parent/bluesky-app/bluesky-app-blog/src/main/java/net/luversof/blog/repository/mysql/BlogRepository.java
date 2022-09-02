package net.luversof.blog.repository.mysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.blog.domain.mysql.Blog;

public interface BlogRepository extends JpaRepository<Blog, Long>/* , QueryByExampleExecutor<Blog> */{

	List<Blog> findByUserId(String userId); 
	
	Optional<Blog> findByBlogId(String blogId);

}
