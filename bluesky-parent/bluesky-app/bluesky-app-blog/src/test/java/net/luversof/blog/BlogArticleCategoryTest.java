package net.luversof.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.service.BlogArticleCategoryService;
import net.luversof.blog.service.BlogService;

@Slf4j
class BlogArticleCategoryTest extends GeneralTest {
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;

	@Test
	@DisplayName("카테고리 생성 테스트")
	void create() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		assertThat(blog).isNotNull();
		
		var blogArticleCategory = new BlogArticleCategory();
		blogArticleCategory.setBlogId(blog.getBlogId());
		blogArticleCategory.setName("카테고리 테스트");
			
		var savedBlogArticleCategory = blogArticleCategoryService.create(blogArticleCategory);
		assertThat(savedBlogArticleCategory).isNotNull();
		log.debug("saveAll : {}", savedBlogArticleCategory);
	}
	
	@Test
	@DisplayName("카테고리 조회 테스트")
	void findByBlogId() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		assertThat(blog).isNotNull();
		
		var blogArticleCategoryList = blogArticleCategoryService.findByBlogId(blog.getBlogId());
		assertThat(blogArticleCategoryList).isNotEmpty();
		
		log.debug("blogArticleCategoryList : {}", blogArticleCategoryList);
	}
	
	@Test
	@DisplayName("카테고리 수정 테스트")
	void updateBlogArticleCategory() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		
		var blogArticleCategoryList = blogArticleCategoryService.findByBlogId(blog.getBlogId());
		assertThat(blogArticleCategoryList).isNotEmpty();
		
		var blogArticleCategory = blogArticleCategoryList.get(0);
		blogArticleCategory.setName("카테고리 수정");
		
		var result = blogArticleCategoryService.update(blogArticleCategory);
		
		log.debug("blogArticleCategory : {}", result);
		
	}
}
