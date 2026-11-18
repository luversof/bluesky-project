package net.luversof.api.blog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.blog.domain.mariadb.Blog;
import net.luversof.api.blog.service.BlogService;

/** 블로그. 사용자마다 블로그 하나를 둔다. */
@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogController implements BlogControllerDocs {

  @Autowired private BlogService blogService;

  /**
   * 블로그를 만든다.
   *
   * @param blog 만들 블로그(userId 필수)
   * @return 만든 블로그(blogId 가 채워진다)
   */
  @Override
  @PostMapping
  public Blog create(@Validated(Blog.Create.class) @RequestBody Blog blog) {
    return blogService.create(blog);
  }

  /**
   * blogId 로 블로그를 찾는다.
   *
   * @param blogId 블로그 ID
   * @return 블로그, 없으면 빈 값
   */
  @GetMapping("/search/findByBlogId/{blogId}")
  public Optional<Blog> findByBlogId(@PathVariable String blogId) {
    return blogService.findByBlogId(blogId);
  }

  /**
   * 사용자의 블로그 목록.
   *
   * @param userId 사용자 ID
   * @return 블로그 목록
   */
  @GetMapping("/search/findByUserId/{userId}")
  public List<Blog> findByUserId(@PathVariable String userId) {
    return blogService.findByUserId(userId);
  }
}
