package net.luversof.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;

@PreAuthorize("hasRole('ROLE_MANAGER')")
@RepositoryRestResource(path = "blogArticles", collectionResourceRel = "blogArticles")
@Transactional(readOnly = true)
public interface ArticleRepository extends JpaRepository<Article, Long> {
	Page<Article> findByBlog(Blog blog, Pageable pageable);
}