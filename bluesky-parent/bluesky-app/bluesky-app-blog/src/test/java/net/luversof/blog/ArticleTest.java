package net.luversof.blog;


import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;
import net.luversof.blog.service.BlogService;

@Slf4j
public class ArticleTest extends GeneralTest {

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private ArticleService articleService;
	
	@Autowired
	private ArticleCategoryService articleCategoryService;
	
	@Autowired
	private BlogService blogService;
	
	private long userId = 1;
	

	@Test
//	@Ignore
	public void selectTest() {
		Article article = articleService.findOne(18);
		log.debug("result : {}", article);
	}
	
	@Test
	public void 카테고리추가글작성테스트() {
		Blog blog = blogService.findByUser(userId);
		for (int i = 0 ; i < 20 ; i ++) {
			
			Article article = new Article();
			article.setBlog(blog);
			article.setTitle("한글제목" + i);
			article.setContent("한글내용" + i);
			
//			ArticleCategory articleCategory = articleCategoryService.findOne(4);
			ArticleCategory articleCategory = new ArticleCategory();
			articleCategory.setBlog(blog);
			articleCategory.setName("바꿨다" + i);
			articleCategory.setId(4);
			//article.setArticleCategory(articleCategory);
			
			Article savedArticle = articleService.save(article);
		}
		
	}
	

	@Test
	public void 글삭제테스트() {
		articleService.delete(11);
	}
	
	
	@Test
	public void 수정전파테스트() {
		Article article = articleService.findOne(11);
		article.setContent("수정했음3");
		
//		ArticleCategory articleCategory = articleCategoryService.findOne(4);
		ArticleCategory articleCategory = new ArticleCategory();
		articleCategory.setBlog(article.getBlog());
		articleCategory.setName("추가32331");
		articleCategory.setId(5);
		article.setArticleCategory(articleCategory);
		
//		article.getArticleCategory().setName("수정2");
		articleService.save(article);
	}
	
	@Test
	//@Ignore
	public void saveTest() {
		Article article = new Article();
		article.setBlog(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
		ArticleCategory articleCategory = articleCategoryService.findOne(1);
		article.setArticleCategory(articleCategory);

		Article savedArticle = articleService.save(article);
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
		Blog blog = blogService.findByUser(userId);
		
		int page = 1;
		
		
		Page<Article> blogList = articleService.findByBlog(blog, page);
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
		List<ArticleCategory> list = articleCategoryService.findByBlog(null);
		
		
		log.debug("list : {}", list);
	}
	
	@Test
	public void 블로그객체비교테스트() {
		Blog blog = blogService.findOne(1);
		Article article = articleService.findOne(1);
		
		System.out.println(blog);
		System.out.println(article.getBlog());
		System.out.println(blog.equals(article.getBlog()));
		
	}
}
