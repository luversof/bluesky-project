package net.luversof.blog.repository.mysql;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.domain.mysql.BlogArticleCategory;

@Transactional(readOnly = true)
public interface BlogArticleCategoryRepository extends JpaRepository<BlogArticleCategory, Long> {
	
	List<BlogArticleCategory> findByBlog(Blog blog);

	List<BlogArticleCategory> findByBlogId(UUID blogId);
}
