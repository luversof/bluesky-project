package net.luversof.blog.service;

import net.luversof.blog.domain.BlogPost;
import net.luversof.blog.repository.BlogPostRepository;
import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BLOG)
public class BlogPostService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private BlogPostRepository blogPostRepository;

	public BlogPost save(BlogPost blogPost) {
		return blogPostRepository.save(blogPost);
	}

	@Transactional(readOnly = true)
	public BlogPost findOne(long id) {
		return blogPostRepository.findOne(id);
	}

	@Transactional(readOnly = true)
	public Page<BlogPost> findAll(int page) {
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(page, PAGE_SIZE, sort);
		return blogPostRepository.findAll(pageRequest);
	}

	public void delete(long id) {
		blogPostRepository.delete(id);
	}
}
