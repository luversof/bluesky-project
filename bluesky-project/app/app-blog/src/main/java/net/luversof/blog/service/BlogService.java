package net.luversof.blog.service;

import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.data.jpa.datasource.DataSource;
import net.luversof.data.jpa.datasource.DataSourceType;
import net.luversof.data.jpa.exception.BlueskyException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class BlogService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private BlogCategoryService blogCategoryService;

	public Blog save(Blog blog) {
		return blogRepository.save(blog);
	}
	
	public Blog update(Blog blog) {
		Blog targetBlog = findOne(blog.getId());
		if (!blog.getUsername().equals(targetBlog.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		targetBlog.setTitle(blog.getTitle());
		targetBlog.setContent(blog.getContent());
		if (blog.getBlogCategory() != null && blog.getBlogCategory().getId() != 0) {
			targetBlog.setBlogCategory(blogCategoryService.findOne(blog.getBlogCategory().getId()));
		}
		return blogRepository.save(targetBlog);
	}

	@Transactional(readOnly = true)
	public Blog findOne(long id) {
		return blogRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public Page<Blog> findAll(int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return blogRepository.findAll(pageRequest);
	}

	public void delete(long id) {
		blogRepository.delete(id);
	}
}
