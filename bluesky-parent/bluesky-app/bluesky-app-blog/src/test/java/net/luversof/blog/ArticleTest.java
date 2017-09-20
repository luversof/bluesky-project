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
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.repository.CategoryRepository;

@Slf4j
public class ArticleTest extends GeneralTest {

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private CategoryRepository categoryRepository;
	
	@Autowired
	private BlogRepository blogRepository ;
	
	private UUID userId = UUID.fromString("77a04682-3032-492c-9449-5ba986491eef");
	

	@Test
//	@Ignore
	public void selectTest() {
		Optional<Article> blogArticleOptional = articleRepository.findById((long) 18);
		log.debug("result : {}", blogArticleOptional.get());
	}
	
	@Test
	public void 카테고리추가글작성테스트() {
		Blog blog = blogRepository.findByUserId(userId).get();
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
			
			Article savedArticle = articleRepository.save(article);
			log.debug("savedArticle : {}", savedArticle);
		}
		
	}
	

	@Test
	public void 글삭제테스트() {
		articleRepository.deleteById((long) 11);
	}
	
	
	@Test
	public void 수정전파테스트() {
		Article article = articleRepository.findById((long) 11).get();
		article.setContent("수정했음3");
		
//		ArticleCategory articleCategory = articleCategoryService.findOne(4);
		Category category = new Category();
		category.setBlog(article.getBlog());
		category.setName("추가32331");
		category.setId(5);
		article.setCategory(category);
		
//		article.getArticleCategory().setName("수정2");
		articleRepository.save(article);
	}
	
	@Test
	//@Ignore
	public void saveTest() {
		Article article = new Article();
		article.setBlog(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
		Category category = categoryRepository.findById((long) 1).orElse(null);
		article.setCategory(category);

		Article savedArticle = articleRepository.save(article);
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

//	@Test
////	@Ignore
//	public void selectPaging테스트() {
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
//	public void 테스트() {
//		QBlog blog = QBlog.blog;
//		Iterable<Article> list = articleRepository.findAll(blog.content.startsWith("c"));
//		log.debug("list : {}", list);
//	}
	
	@Test
	public void 블로그카테고리테스트() {
		List<Category> list = categoryRepository.findByBlog(null);
		
		
		log.debug("list : {}", list);
	}
	
	@Test
	public void 블로그객체비교테스트() {
		Blog blog = blogRepository.findById(UUID.randomUUID()).get();
		Article article = articleRepository.findById((long) 1).get();
		
		System.out.println(blog);
		System.out.println(article.getBlog());
		System.out.println(blog.equals(article.getBlog()));
		
	}
}
