package net.luversof.blog.repository.mysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.Blog;

@Transactional(readOnly = true)
public interface BlogRepository extends JpaRepository<Blog, Long> {

	List<Blog> findByUserId(String userId); 
	
	Optional<Blog> findByBlogId(String blogId);

}
