package net.luversof.blog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Category;
import net.luversof.blog.domain.Blog;

@PreAuthorize("hasRole('ROLE_USER')")
@Transactional(readOnly = true)
public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByBlog(@Param("blog") Blog blog);

	List<Category> findByBlogId(@Param("blogId") UUID blogId);
}
