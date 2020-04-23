package net.luversof.blog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticleCategory;

@Transactional(readOnly = true)
public interface BlogArticleCategoryRepository extends JpaRepository<BlogArticleCategory, Long> {
	
	List<BlogArticleCategory> findByBlog(Blog blog);

	List<BlogArticleCategory> findByBlogId(UUID blogId);
}
