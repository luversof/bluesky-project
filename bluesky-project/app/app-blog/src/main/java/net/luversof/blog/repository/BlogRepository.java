package net.luversof.blog.repository;

import net.luversof.blog.domain.Blog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long>, QueryDslPredicateExecutor<Blog> {
}