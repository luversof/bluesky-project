package net.luversof.blog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogCategory;

@Transactional(readOnly = true)
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {
	List<BlogCategory> findByBlog(@Param("blog") Blog blog);

	List<BlogCategory> findByBlogId(@Param("blogId") UUID blogId);
}
