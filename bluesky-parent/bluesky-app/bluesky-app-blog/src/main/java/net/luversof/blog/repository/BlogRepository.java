package net.luversof.blog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;

//@PreAuthorize("hasRole('ROLE_USER')")
@Transactional(readOnly = true)
public interface BlogRepository extends JpaRepository<Blog, UUID> {
	@PreAuthorize("hasRole('ROLE_USER')")
	Optional<Blog> findByUserId(@Param("userId") UUID userId); 
}
