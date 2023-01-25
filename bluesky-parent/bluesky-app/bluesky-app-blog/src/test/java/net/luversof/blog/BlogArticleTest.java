package net.luversof.blog;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.Blog;
import net.luversof.blog.domain.mysql.BlogArticle;
import net.luversof.blog.domain.mysql.BlogArticleCategory;
import net.luversof.blog.repository.mysql.BlogArticleRepository;
import net.luversof.blog.service.BlogArticleCategoryService;
import net.luversof.blog.service.BlogArticleService;
import net.luversof.blog.service.BlogService;

@Slf4j
@Transactional
@Rollback(false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BlogArticleTest implements GeneralTest {

	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogArticleService blogArticleService;
	
	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;
	
	
	@Test
	@Order(1)
	@DisplayName("글 쓰기 테스트")
	void create() {
		
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		
		var blogArticleCategoryList = blogArticleCategoryService.findByBlogId(blog.getBlogId());
		
		
		BlogArticle blogArticle = new BlogArticle();
		blogArticle.setUserId(blog.getUserId());
		blogArticle.setTitle("글 제목");
		blogArticle.setContent("글 내용");
		blogArticle.setBlogId(blog.getBlogId());
		
		if (!blogArticleCategoryList.isEmpty()) {
			blogArticle.setBlogArticleCategory(blogArticleCategoryList.get(new Random().nextInt(blogArticleCategoryList.size())));
		}
		
		log.debug("save : {}", blogArticleService.create(blogArticle));
	}
	
	@Test
	@Order(2)
	@DisplayName("글 목록 보기 테스트")
	void findByBlogId() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		var blogArticleList = blogArticleService.findByBlogId(blog.getBlogId(), PageRequest.of(0, 10));
		assertThat(blogArticleList).isNotNull();
		log.debug("blogArticleList : {}", blogArticleList);
	}

	@Test
	@Order(3)
	@DisplayName("글 보기 테스트")
//	@Ignore
//	@WithMockUser
	void findByBlogArticleId() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		var blogArticlePaging = blogArticleService.findByBlogId(blog.getBlogId(), PageRequest.of(0, 10));
		var targetBlogArticle = blogArticlePaging.get().findFirst().get();
		
		var blogArticleOptional = blogArticleService.findByBlogArticleId(targetBlogArticle.getBlogArticleId());
		
		assertThat(blogArticleOptional).isNotNull();
		
		log.debug("result : {}", blogArticleOptional.get());
	}


	
	@Test
	@Disabled("임시 제거")
	void 카테고리추가복수글작성테스트() {
		Blog blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		for (int i = 0 ; i < 1024 ; i ++) {
			
			BlogArticle article = new BlogArticle();
			article.setBlogId(blog.getBlogId());
			article.setTitle("한글제목" + i);
			article.setContent("한글내용" + i);
			
//			ArticleCategory articleCategory = articleCategoryService.findOne(4);
			BlogArticleCategory articleCategory = new BlogArticleCategory();
			articleCategory.setBlogId(blog.getBlogId());
			articleCategory.setName("바꿨다" + i);
			articleCategory.setIdx(4);
			//article.setArticleCategory(articleCategory);
			
			BlogArticle savedArticle = blogArticleRepository.save(article);
			log.debug("savedArticle : {}", savedArticle);
		}
		
	}
	

	@Test
	@Order(4)
	@DisplayName("글 삭제 테스트")
	void delete() {
		var blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		var blogArticlePaging = blogArticleService.findByBlogId(blog.getBlogId(), PageRequest.of(0, 10));
		var targetBlogArticle = blogArticlePaging.get().findFirst().get();
		
		BlogArticle blogArticle = new BlogArticle();
		blogArticle.setBlogArticleId(targetBlogArticle.getBlogArticleId());
		blogArticle.setUserId(targetBlogArticle.getUserId());
		blogArticleService.delete(blogArticle);
		
	}
	
}
