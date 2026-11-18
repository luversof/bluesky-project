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

import net.luversof.api.blog.domain.mariadb.BlogArticle;
import net.luversof.api.blog.service.BlogArticleService;

/** 블로그 글. */
@RestController
@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

  @Autowired private BlogArticleService blogArticleService;

  /**
   * 글을 쓴다.
   *
   * @param blogArticle 쓸 글
   * @return 쓴 글
   */
  @PostMapping
  public BlogArticle create(
      @Validated(BlogArticle.Create.class) @RequestBody BlogArticle blogArticle) {
    return blogArticleService.create(blogArticle);
  }

  /**
   * 블로그의 글 목록.
   *
   * @param blogId 블로그 ID
   * @param pageable 페이지(page·size·sort)
   * @return 글 한 페이지
   */
  @GetMapping("/search/findByBlogId/{blogId}")
  public Page<BlogArticle> findByBlogId(@PathVariable String blogId, Pageable pageable) {
    return blogArticleService.findByBlogId(blogId, pageable);
  }

  /**
   * 글 하나를 찾는다.
   *
   * @param blogArticleId 글 ID
   * @return 글, 없으면 빈 값
   */
  @GetMapping("/search/findByBlogArticleId/{blogArticleId}")
  public Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId) {
    return blogArticleService.findByBlogArticleId(blogArticleId);
  }

  /**
   * 글을 고친다. 쓴 사람만 고칠 수 있다.
   *
   * @param blogArticle 고칠 글
   * @return 고친 글
   */
  @PutMapping
  public BlogArticle update(
      @Validated(BlogArticle.Update.class) @RequestBody BlogArticle blogArticle) {
    return blogArticleService.update(blogArticle);
  }

  /**
   * 글을 지운다. 쓴 사람만 지울 수 있다.
   *
   * @param blogArticle 지울 글
   */
  @DeleteMapping
  public void delete(@Validated(BlogArticle.Delete.class) @RequestBody BlogArticle blogArticle) {
    blogArticleService.delete(blogArticle);
  }
}
