package net.luversof.blog;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.Category;
import net.luversof.blog.service.CategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;

@Slf4j
public class ArticleTest extends GeneralTest {

	@Autowired
	private ArticleService blogArticleService;
	
	@Autowired
	private CategoryService blogArticleCategoryService;
	
	@Autowired
	private BlogService blogService;
	
	private UUID userId = UUID.fromString("77a04682-3032-492c-9449-5ba986491eef");
	

	@Test
//	@Ignore
	public void selectTest() {
		Optional<Article> blogArticleOptional = blogArticleService.findById(18);
		log.debug("result : {}", blogArticleOptional.get());
	}
	
	@Test
	public void 카테고리추가글작성테스트() {
		Blog blog = blogService.findByUserId(userId).get();
		for (int i = 0 ; i < 1024 ; i ++) {
			
			Article article = new Article();
			article.setBlog(blog);
			article.setTitle("한글제목" + i);
			article.setContent("한글내용" + i);
			
//			ArticleCategory articleCategory = articleCategoryService.findOne(4);
			Category articleCategory = new Category();
			articleCategory.setBlog(blog);
			articleCategory.setName("바꿨다" + i);
			articleCategory.setId(4);
			//article.setArticleCategory(articleCategory);
			
			Article savedArticle = blogArticleService.save(article);
			log.debug("savedArticle : {}", savedArticle);
		}
		
	}
	

	@Test
	public void 글삭제테스트() {
		blogArticleService.delete(11);
	}
	
	
	@Test
	public void 수정전파테스트() {
		Article article = blogArticleService.findById(11).get();
		article.setContent("수정했음3");
		
//		ArticleCategory articleCategory = articleCategoryService.findOne(4);
		Category category = new Category();
		category.setBlog(article.getBlog());
		category.setName("추가32331");
		category.setId(5);
		article.setCategory(category);
		
//		article.getArticleCategory().setName("수정2");
		blogArticleService.save(article);
	}
	
	@Test
	//@Ignore
	public void saveTest() {
		Article article = new Article();
		article.setBlog(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
		Category category = blogArticleCategoryService.findOne(1);
		article.setCategory(category);

		Article savedArticle = blogArticleService.save(article);
		log.debug("article : {}", article);
		log.debug("savedBlog : {}", savedArticle);
		log.debug("savedBlog : {}", savedArticle.getId());
	}
	
	@Test
//	@Ignore
	public void 대량save테스트() {
		for (int i = 0 ; i < 11 ; i++) {
			saveTest();
		}
	}

	@Test
//	@Ignore
	public void selectPaging테스트() {
		Blog blog = blogService.findByUserId(userId).get();
		
		Pageable page = PageRequest.of(1, 10);
		Page<Article> blogList = blogArticleService.findByBlog(blog, page);
		log.debug("blogList : {}", blogList);
		log.debug("blogList : {}", blogList.getContent());
		
	}
	
//	@Test
//	@Ignore
//	public void 테스트() {
//		QBlog blog = QBlog.blog;
//		Iterable<Article> list = articleRepository.findAll(blog.content.startsWith("c"));
//		log.debug("list : {}", list);
//	}
	
	@Test
	public void 블로그카테고리테스트() {
		List<Category> list = blogArticleCategoryService.findByBlog(null);
		
		
		log.debug("list : {}", list);
	}
	
	@Test
	public void 블로그객체비교테스트() {
		Blog blog = blogService.findById(UUID.randomUUID()).get();
		Article article = blogArticleService.findById(1).get();
		
		System.out.println(blog);
		System.out.println(article.getBlog());
		System.out.println(blog.equals(article.getBlog()));
		
	}
}
