package net.luversof.blog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Category;
import net.luversof.blog.domain.Blog;

@Transactional(readOnly = true)
public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByBlog(Blog blog);

	List<Category> findByBlogId(UUID id);
}
