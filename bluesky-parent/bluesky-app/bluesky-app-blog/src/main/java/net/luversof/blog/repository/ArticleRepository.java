package net.luversof.blog.repository;

import net.luversof.blog.domain.Article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, QueryDslPredicateExecutor<Article> {
}