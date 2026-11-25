package net.luversof.api.blog.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.blog.domain.mariadb.BlogArticleComment;
import net.luversof.api.blog.service.BlogArticleCommentService;

/** 블로그 글 댓글 */
@RestController
@RequestMapping(value = "/api/blogArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCommentController {

  @Autowired private BlogArticleCommentService blogArticleCommentService;

  /**
   * 블로그 글 댓글 작성
   *
   * @param blogArticleComment 달 댓글
   * @return 단 댓글
   */
  @PostMapping
  public BlogArticleComment create(
      @Validated(BlogArticleComment.Create.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    return blogArticleCommentService.create(blogArticleComment);
  }

  /**
   * 블로그 글의 댓글 목록 조회
   *
   * @param blogArticleId 글 ID
   * @param pageable 페이지(page·size·sort)
   * @return 댓글 한 페이지
   */
  @GetMapping("/search/findByBlogArticleId/{blogArticleId}")
  public Page<BlogArticleComment> findByBlogArticleId(
      @PathVariable String blogArticleId, Pageable pageable) {
    return blogArticleCommentService.findByBlogArticleId(blogArticleId, pageable);
  }

  /**
   * 블로그 글 댓글 조회
   *
   * @param blogArticleCommentId 댓글 ID
   * @return 댓글, 없으면 빈 값
   */
  @GetMapping("/search/findByBlogArticleCommentId/{blogArticleCommentId}")
  public Optional<BlogArticleComment> findByBlogArticleCommentId(
      @PathVariable String blogArticleCommentId) {
    return blogArticleCommentService.findByBlogArticleCommentId(blogArticleCommentId);
  }

  /**
   * 블로그 글의 댓글 수 조회
   *
   * @param blogArticleId 글 ID
   * @return 댓글 수
   */
  @GetMapping("/search/countByBlogArticleId/{blogArticleId}")
  public long countByBlogArticleId(@PathVariable String blogArticleId) {
    return blogArticleCommentService.countByBlogArticleId(blogArticleId);
  }

  /**
   * 블로그 글 댓글 수정
   *
   * @param blogArticleComment 고칠 댓글
   * @return 고친 댓글
   */
  @PutMapping
  public BlogArticleComment update(
      @Validated(BlogArticleComment.Update.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    return blogArticleCommentService.update(blogArticleComment);
  }

  /**
   * 블로그 글 댓글 삭제
   *
   * @param blogArticleComment 지울 댓글
   */
  @DeleteMapping
  public void delete(
      @Validated(BlogArticleComment.Delete.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    blogArticleCommentService.delete(blogArticleComment);
  }
}
