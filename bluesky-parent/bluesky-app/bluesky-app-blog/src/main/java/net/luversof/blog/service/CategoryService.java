package net.luversof.blog.service;

import java.util.List;

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

	public Category findOne(long id) {
		return categoryRepository.getOne(id);
	}

	public List<Category> findByBlog(Blog blog) {
		return categoryRepository.findByBlog(blog);
	}

	public void delete(long id) {
		categoryRepository.deleteById(id);
	}
}
