package net.luversof.blog.repository;

import java.util.List;

import net.luversof.blog.domain.BlogCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long>, QueryDslPredicateExecutor<BlogCategory> {
	List<BlogCategory> findByUsername(String username);
}
