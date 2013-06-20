package net.luversof.blog.repository;

import net.luversof.blog.domain.Blog;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository("blogRepository")
public interface BlogRepository extends PagingAndSortingRepository<Blog, Long> {
}