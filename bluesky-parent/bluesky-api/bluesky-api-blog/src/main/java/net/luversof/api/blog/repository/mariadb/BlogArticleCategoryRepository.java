package net.luversof.api.blog.repository.mariadb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.blog.domain.mariadb.BlogArticleCategory;

public interface BlogArticleCategoryRepository extends JpaRepository<BlogArticleCategory, Long> {
	
	Optional<BlogArticleCategory> findByBlogArticleCategoryId(String blogArticleCategoryId);
	
	List<BlogArticleCategory> findByBlogId(String blogId);

}
