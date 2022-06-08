package net.luversof.blog;


import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

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
class BlogArticleTest extends GeneralTest {

	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private BlogArticleService blogArticleService;
	
	@Autowired
	private BlogArticleCategoryService blogArticleCategoryService;
	
	
	@Test
	@DisplayName("글 쓰기 테스트")
	void writeBlogArticle() {
		
		Blog blog = blogService.findByUserId(BlogTestConstant.USER_ID).stream().findFirst().get();
		
		List<BlogArticleCategory> blogArticleCategoryList = blogArticleCategoryService.findByBlogId(blog.getBlogId());
		
		
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
	@DisplayName("글 보기 테스트")
//	@Ignore
	@WithMockUser
	void selectTest() {
		Optional<BlogArticle> blogArticleOptional = blogArticleRepository.findById((long) 2);
		log.debug("result : {}", blogArticleOptional.get());
	}


	
	@Test
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
	void 글삭제테스트() {
		blogArticleRepository.deleteById((long) 11);
	}
	
	
	@Test
	void 수정전파테스트() {
		BlogArticle article = blogArticleRepository.findById((long) 11).get();
		article.setContent("수정했음3");
		
//		ArticleCategory articleCategory = articleCategoryService.findOne(4);
		BlogArticleCategory category = new BlogArticleCategory();
		category.setBlogId(article.getBlogId());
		category.setName("추가32331");
		category.setIdx(5);
		article.setBlogArticleCategory(category);
		
//		article.getArticleCategory().setName("수정2");
		blogArticleRepository.save(article);
	}
	
	@Test
	//@Ignore
	void saveTest() {
		BlogArticle article = new BlogArticle();
		article.setBlogId(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
//		BlogArticleCategory category = blogArticleCategoryService.findById((long) 1).orElse(null);
//		article.setBlogArticleCategory(category);

		BlogArticle savedArticle = blogArticleRepository.save(article);
		log.debug("article : {}", article);
		log.debug("savedBlog : {}", savedArticle);
		log.debug("savedBlog : {}", savedArticle.getIdx());
	}
	
	@Test
//	@Ignore
	void 대량save테스트() {
		for (int i = 0 ; i < 11 ; i++) {
			saveTest();
		}
	}

//	@Test
////	@Ignore
//	void selectPaging테스트() {
//		Blog blog = blogRepository.findByUserId(userId).get();
//		
//		Pageable page = PageRequest.of(1, 10);
//		Page<Article> blogList = articleRepository.findByBlog(blog, page);
//		log.debug("blogList : {}", blogList);
//		log.debug("blogList : {}", blogList.getContent());
//		
//	}
	
//	@Test
//	@Ignore
//	void 테스트() {
//		QBlog blog = QBlog.blog;
//		Iterable<Article> list = articleRepository.findAll(blog.content.startsWith("c"));
//		log.debug("list : {}", list);
//	}
	
	@Test
	void 블로그카테고리테스트() {
		List<BlogArticleCategory> list = blogArticleCategoryService.findByBlogId(null);
		
		
		log.debug("list : {}", list);
	}
	
}
