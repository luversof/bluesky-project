package net.luversof.blog.repository;

import net.luversof.blog.domain.BlogCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long>, QueryDslPredicateExecutor<BlogCategory> {
}
