package net.luversof.blog.repository.mysql;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.Blog;

@Transactional(readOnly = true)
public interface BlogRepository extends JpaRepository<Blog, Long> {

	Optional<Blog> findByUserId(String userId); 

}
