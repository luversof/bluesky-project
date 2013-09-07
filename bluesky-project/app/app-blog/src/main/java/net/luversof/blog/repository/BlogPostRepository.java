package net.luversof.blog.repository;

import net.luversof.blog.domain.BlogPost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long>, QueryDslPredicateExecutor<BlogPost> {
}