package net.luversof.blog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.blog.domain.Category;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.CategoryRepository;

@Service
public class CategoryService {

	@Autowired
	private CategoryRepository categoryRepository;

	public Category save(Category articleCategory) {
		return categoryRepository.save(articleCategory);
	}

	public Optional<Category> findById(long id) {
		return categoryRepository.findById(id);
	}

	public List<Category> findByBlog(Blog blog) {
		return categoryRepository.findByBlog(blog);
	}
	
	public List<Category> findByBlogId(UUID id) {
		return categoryRepository.findByBlogId(id);
	}

	public void delete(long id) {
		categoryRepository.deleteById(id);
	}
}
