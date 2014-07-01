package net.luversof.blog;


import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.blog.domain.Article;
import net.luversof.blog.domain.ArticleCategory;
import net.luversof.blog.repository.ArticleRepository;
import net.luversof.blog.service.ArticleCategoryService;
import net.luversof.blog.service.ArticleService;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Slf4j
public class BlogTest extends GeneralTest {

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private ArticleService articleService;
	
	@Autowired
	private ArticleCategoryService blogCategoryService;
	

	@Test
//	@Ignore
	public void selectTest() {
		Article blog = articleService.findOne(28);
		log.debug("result : {}", blog);
	}

	@Test
	//@Ignore
	public void saveTest() {
		Article article = new Article();
		article.setBlog(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
		ArticleCategory articleCategory = blogCategoryService.findOne(1);
		article.setArticleCategory(articleCategory);

		Article savedBlog = articleService.save(article);
		log.debug("blog : {}", article);
		log.debug("savedBlog : {}", savedBlog);
		log.debug("savedBlog : {}", savedBlog.getId());
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
		Page<Article> blogList = articleService.findAll(0);
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
		List<ArticleCategory> list = blogCategoryService.findByBlog(null);
		
		
		log.debug("list : {}", list);
	}
}
