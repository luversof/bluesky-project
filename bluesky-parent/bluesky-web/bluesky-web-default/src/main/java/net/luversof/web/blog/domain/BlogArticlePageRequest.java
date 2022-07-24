package net.luversof.web.blog.domain;

import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.Data;

@Data
public class BlogArticlePageRequest {
	
	@Range(min = 0)
	private int page;
	
	@Range(min = 1, max = 100)
	private int size = 10;
	
	private Sort sort = Sort.by("idx").descending();

	public PageRequest toPageRequest() {
		return PageRequest.of(page, size, sort);
	}

}
