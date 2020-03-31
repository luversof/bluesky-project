package net.luversof.blog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;

@Transactional(readOnly = true)
public interface BlogRepository extends JpaRepository<Blog, UUID> {

	Optional<Blog> findByUserId(UUID userId); 

}
