package net.luversof.blog;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.repository.mysql.BlogArticleCategoryRepository;
import net.luversof.blog.repository.mysql.BlogRepository;

@Slf4j
public class BlogArticleCategoryTest extends GeneralTest {
	
	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	@Autowired
	private BlogRepository blogRepository;

	@Test
	@DisplayName("카테고리 저장 테스트")
	public void saveBlogArticleCategory() {
		// 1. 대상 blog 조회
		Blog blog = blogRepository.findAll().get(0);
		
		List<BlogArticleCategory> blogArticleCategoryList = new ArrayList<>();
		
		// 2. 저장 목록 생성
		for (int i = 0; i < 10; i++) {
			BlogArticleCategory blogArticleCategory = new BlogArticleCategory();
			
			blogArticleCategory.setBlog(blog);
			blogArticleCategory.setName("카테고리 " + i);
			blogArticleCategoryList.add(blogArticleCategory);
			
		}
		
		// 3. 저장
		log.debug("saveAll : {}", blogArticleCategoryRepository.saveAll(blogArticleCategoryList));
		
	}
}
