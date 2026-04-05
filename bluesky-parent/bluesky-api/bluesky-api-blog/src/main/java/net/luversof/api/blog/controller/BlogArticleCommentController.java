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

@RestController
@RequestMapping(value = "/api/blogArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleCommentController {

  @Autowired private BlogArticleCommentService blogArticleCommentService;

  @PostMapping
  public BlogArticleComment create(
      @Validated(BlogArticleComment.Create.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    return blogArticleCommentService.create(blogArticleComment);
  }

  @GetMapping("/search/findByBlogArticleId/{blogArticleId}")
  public Page<BlogArticleComment> findByBlogArticleId(
      @PathVariable String blogArticleId, Pageable pageable) {
    return blogArticleCommentService.findByBlogArticleId(blogArticleId, pageable);
  }

  @GetMapping("/search/findByBlogArticleCommentId/{blogArticleCommentId}")
  public Optional<BlogArticleComment> findByBlogArticleCommentId(
      @PathVariable String blogArticleCommentId) {
    return blogArticleCommentService.findByBlogArticleCommentId(blogArticleCommentId);
  }

  @GetMapping("/search/countByBlogArticleId/{blogArticleId}")
  public long countByBlogArticleId(@PathVariable String blogArticleId) {
    return blogArticleCommentService.countByBlogArticleId(blogArticleId);
  }

  @PutMapping
  public BlogArticleComment update(
      @Validated(BlogArticleComment.Update.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    return blogArticleCommentService.update(blogArticleComment);
  }

  @DeleteMapping
  public void delete(
      @Validated(BlogArticleComment.Delete.class) @RequestBody
          BlogArticleComment blogArticleComment) {
    blogArticleCommentService.delete(blogArticleComment);
  }
}
