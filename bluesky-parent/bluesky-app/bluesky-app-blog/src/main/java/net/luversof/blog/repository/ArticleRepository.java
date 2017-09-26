package net.luversof.blog.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.blog.domain.Article;
import net.luversof.blog.projection.ArticleProjection;

@PreAuthorize("hasRole('ROLE_USER')")
@RepositoryRestResource(path = "blogArticles", collectionResourceRel = "blogArticles", collectionResourceDescription = @Description("아티클리포지토리collection"), itemResourceDescription = @Description("아티클리포지토리item"), excerptProjection = ArticleProjection.class)
@Transactional(readOnly = true)
public interface ArticleRepository extends JpaRepository<Article, Long> {
	
	@RestResource(description = @Description("파인드바이블로그"))
	Page<Article> findByBlogId(@Param("id") UUID id, Pageable pageable);
	
}	