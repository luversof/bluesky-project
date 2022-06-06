package net.luversof.blog.repository.mysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.mysql.BlogArticleCategory;

@Transactional(readOnly = true)
public interface BlogArticleCategoryRepository extends JpaRepository<BlogArticleCategory, Long> {
	
	Optional<BlogArticleCategory> findByBlogArticleCategoryId(String blogArticleCategoryId);
	
	List<BlogArticleCategory> findByBlogId(String blogId);

}
