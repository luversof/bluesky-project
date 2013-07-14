package net.luversof.blog.service;

import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.repository.BlogPostRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class BlogPostService {
	
	@Autowired
	private BlogPostRepository blogPostRepository;
	
	private static final int PAGE_SIZE = 10;
	
	public void save(BlogPost blogPost) {
		blogPostRepository.save(blogPost);
	}
	
	public BlogPost view(long id) {
		return blogPostRepository.findOne(id);
	}

	public Page<BlogPost> list(int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return blogPostRepository.findAll(pageRequest);
	}
	
	public void delete(long id) {
		blogPostRepository.delete(id);
	}
}
