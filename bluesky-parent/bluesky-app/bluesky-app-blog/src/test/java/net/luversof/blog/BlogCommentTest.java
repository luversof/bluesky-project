package net.luversof.blog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.BlogArticle;
import net.luversof.blog.domain.BlogComment;
import net.luversof.blog.repository.BlogArticleRepository;
import net.luversof.blog.repository.BlogCommentRepository;

@Slf4j
public class BlogCommentTest extends GeneralTest {

	@Autowired
	private BlogArticleRepository blogArticleRepository;

	@Autowired
	private BlogCommentRepository blogCommentRepository;

	@DisplayName("댓글 목록 조회")
	@Test
	public void blogCommentList() {
		BlogArticle blogArticle = blogArticleRepository.findAll().get(0);
		log.debug("test : {}", blogCommentRepository.findByBlogArticleId(blogArticle.getId(), PageRequest.of(0,  10)));
	}
	
	@DisplayName("댓글 쓰기")
	@Test
	public void blogCommentWrite() {
		BlogArticle blogArticle = blogArticleRepository.findAll().get(0);
		
		BlogComment blogComment = new BlogComment();
		blogComment.setBlogArticle(blogArticle);
		
		blogComment.setComment("TEST");
		
		log.debug("result : {}", blogCommentRepository.save(blogComment));
	}
}
