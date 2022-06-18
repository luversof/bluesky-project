package net.luversof.blog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mysql.BlogArticle;
import net.luversof.blog.domain.mysql.BlogArticleComment;
import net.luversof.blog.repository.mysql.BlogArticleRepository;
import net.luversof.blog.repository.mysql.BlogArticleCommentRepository;

@Slf4j
public class BlogArticleCommentTest extends GeneralTest {

	@Autowired
	private BlogArticleRepository blogArticleRepository;

	@Autowired
	private BlogArticleCommentRepository blogCommentRepository;

	@DisplayName("댓글 목록 조회")
	@Test
	public void blogCommentList() {
		BlogArticle blogArticle = blogArticleRepository.findAll().get(0);
		log.debug("test : {}", blogCommentRepository.findByBlogArticleId(blogArticle.getBlogArticleId(), PageRequest.of(0,  10)));
	}
	
	@DisplayName("댓글 쓰기")
	@Test
	public void blogCommentWrite() {
		BlogArticle blogArticle = blogArticleRepository.findAll().get(0);
		
		BlogArticleComment blogComment = new BlogArticleComment();
		blogComment.setBlogArticleId(blogArticle.getBlogArticleId());
		
		blogComment.setComment("TEST");
		
		log.debug("result : {}", blogCommentRepository.save(blogComment));
	}
}