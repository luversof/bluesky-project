package net.luversof.api.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import net.luversof.api.blog.domain.mariadb.BlogArticleCategory;
import net.luversof.api.blog.service.BlogArticleCategoryService;

/** 블로그 글 분류. */
@RestController
@RequestMapping(value = "/api/blogArticleCategory", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCategoryController {

  @Autowired private BlogArticleCategoryService blogArticleCategoryService;

  /**
   * 분류를 만든다.
   *
   * @param blogArticleCategory 만들 분류
   * @return 만든 분류
   */
  @PostMapping
  public BlogArticleCategory create(
      @Validated(BlogArticleCategory.Create.class) @RequestBody
          BlogArticleCategory blogArticleCategory) {
    return blogArticleCategoryService.create(blogArticleCategory);
  }

  /**
   * 블로그의 분류 목록.
   *
   * @param blogId 블로그 ID
   * @return 분류 목록
   */
  @GetMapping("/search/findByBlogId/{blogId}")
  public List<BlogArticleCategory> findByBlogId(@PathVariable String blogId) {
    return blogArticleCategoryService.findByBlogId(blogId);
  }

  /**
   * 분류를 고친다.
   *
   * @param blogArticleCategory 고칠 분류
   * @return 고친 분류
   */
  @PutMapping
  public BlogArticleCategory update(
      @Validated(BlogArticleCategory.Update.class) @RequestBody
          BlogArticleCategory blogArticleCategory) {
    return blogArticleCategoryService.update(blogArticleCategory);
  }

  /**
   * 분류를 지운다.
   *
   * @param blogArticleCategory 지울 분류
   */
  @DeleteMapping
  public void delete(
      @Validated(BlogArticleCategory.Delete.class) @RequestBody
          BlogArticleCategory blogArticleCategory) {
    blogArticleCategoryService.delete(blogArticleCategory);
  }
}
