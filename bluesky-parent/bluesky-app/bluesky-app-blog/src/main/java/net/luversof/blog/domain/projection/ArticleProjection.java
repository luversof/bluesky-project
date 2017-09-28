package net.luversof.blog.domain.projection;

import java.time.LocalDateTime;

import org.springframework.data.rest.core.config.Projection;

import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Blog;

@Projection(name = "articleProjection", types = Article.class)
public interface ArticleProjection {

	long getId();

	String getTitle();

	String getContent();

	LocalDateTime getCreatedDate();

	LocalDateTime getLastModifiedDate();

	long getViewCount();

	Blog getBlog();
}
