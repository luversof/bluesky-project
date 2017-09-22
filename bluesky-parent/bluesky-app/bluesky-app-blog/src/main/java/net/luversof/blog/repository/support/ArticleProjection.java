package net.luversof.blog.repository.support;

import org.springframework.data.rest.core.config.Projection;

import net.luversof.blog.domain.Article;

@Projection(name = "articleProjection", types = Article.class)
public class ArticleProjection extends Article {

}
