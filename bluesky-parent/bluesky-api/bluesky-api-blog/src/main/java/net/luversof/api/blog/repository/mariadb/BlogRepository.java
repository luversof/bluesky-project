package net.luversof.api.blog.repository.mariadb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.blog.domain.mariadb.Blog;

public interface BlogRepository extends JpaRepository<Blog, Long>/* , QueryByExampleExecutor<Blog> */{

	List<Blog> findByUserId(String userId); 
	
	Optional<Blog> findByBlogId(String blogId);

}
