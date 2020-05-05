package net.luversof.web.blog.domain;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.Data;

@Data
public class BlogArticlePageRequest {
	
	private int page;
	private int size = 10;
	private Sort sort = Sort.by("id").descending();

	public PageRequest toPageRequest() {
		return PageRequest.of(page, size, sort);
	}

}
