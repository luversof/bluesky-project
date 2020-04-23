package net.luversof.blog;


import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.domain.Blog;
import net.luversof.blog.domain.BlogArticleCategory;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.blog.repository.BlogRepository;
import net.luversof.blog.repository.BlogArticleCategoryRepository;

@Slf4j
public class BlogArticleTest extends GeneralTest {

	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	private BlogArticleRepository blogArticleRepository;
	
	@Autowired
	private BlogArticleCategoryRepository blogArticleCategoryRepository;
	
	
	private UUID userId = UUID.fromString("77a04682-3032-492c-9449-5ba986491eef");
	

	@Test
	@DisplayName("글 보기 테스트")
//	@Ignore
	@WithMockUser
	public void selectTest() {
		Optional<BlogArticle> blogArticleOptional = blogArticleRepository.findById((long) 2);
		log.debug("result : {}", blogArticleOptional.get());
	}

	
	@Test
	@DisplayName("글 쓰기 테스트")
	public void writeBlogArticle() {
		
		Blog blog = blogRepository.findAll().get(0);
		
		List<BlogArticleCategory> blogArticleCategoryList = blogArticleCategoryRepository.findByBlog(blog);
		
		
		BlogArticle blogArticle = new BlogArticle();
		blogArticle.setTitle("글 제목");
		blogArticle.setContent("글 내용");
		blogArticle.setBlog(blog);
		
		if (!blogArticleCategoryList.isEmpty()) {
			blogArticle.setBlogArticleCategory(blogArticleCategoryList.get(new Random().nextInt(blogArticleCategoryList.size())));
		}
		
		log.debug("save : {}", blogArticleRepository.save(blogArticle));
	}
	
	@Test
	public void 카테고리추가복수글작성테스트() {
		Blog blog = blogRepository.findByUserId(userId).get();
		for (int i = 0 ; i < 1024 ; i ++) {
			
			BlogArticle article = new BlogArticle();
			article.setBlog(blog);
			article.setTitle("한글제목" + i);
			article.setContent("한글내용" + i);
			
//			ArticleCategory articleCategory = articleCategoryService.findOne(4);
			BlogArticleCategory articleCategory = new BlogArticleCategory();
			articleCategory.setBlog(blog);
			articleCategory.setName("바꿨다" + i);
			articleCategory.setId(4);
			//article.setArticleCategory(articleCategory);
			
			BlogArticle savedArticle = blogArticleRepository.save(article);
			log.debug("savedArticle : {}", savedArticle);
		}
		
	}
	

	@Test
	public void 글삭제테스트() {
		blogArticleRepository.deleteById((long) 11);
	}
	
	
	@Test
	public void 수정전파테스트() {
		BlogArticle article = blogArticleRepository.findById((long) 11).get();
		article.setContent("수정했음3");
		
//		ArticleCategory articleCategory = articleCategoryService.findOne(4);
		BlogArticleCategory category = new BlogArticleCategory();
		category.setBlog(article.getBlog());
		category.setName("추가32331");
		category.setId(5);
		article.setBlogArticleCategory(category);
		
//		article.getArticleCategory().setName("수정2");
		blogArticleRepository.save(article);
	}
	
	@Test
	//@Ignore
	public void saveTest() {
		BlogArticle article = new BlogArticle();
		article.setBlog(null);
		article.setTitle("한글제목");
		article.setContent("한글내용");
		
		BlogArticleCategory category = blogArticleCategoryRepository.findById((long) 1).orElse(null);
		article.setBlogArticleCategory(category);

		BlogArticle savedArticle = blogArticleRepository.save(article);
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
		List<BlogArticleCategory> list = blogArticleCategoryRepository.findByBlog(null);
		
		
		log.debug("list : {}", list);
	}
	
	@Test
	public void 블로그객체비교테스트() {
		Blog blog = blogRepository.findById(UUID.randomUUID()).get();
		BlogArticle article = blogArticleRepository.findById((long) 1).get();
		
		System.out.println(blog);
		System.out.println(article.getBlog());
		System.out.println(blog.equals(article.getBlog()));
		
	}
}
