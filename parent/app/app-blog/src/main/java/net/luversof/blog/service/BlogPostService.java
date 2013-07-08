package net.luversof.blog.service;

import java.util.List;

import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.repository.BlogPostRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogPostService {
	
	@Autowired
	private BlogPostRepository blogPostRepository;
	
	public void save(BlogPost blog) {
		blogPostRepository.save(blog);
	}
	
	public BlogPost view(Long id) {
		return blogPostRepository.findOne(id);
	}

	public List<BlogPost> list() {
		return blogPostRepository.findAll();
	}
}
