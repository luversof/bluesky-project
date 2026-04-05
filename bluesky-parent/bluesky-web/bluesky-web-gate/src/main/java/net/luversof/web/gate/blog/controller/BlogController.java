package net.luversof.web.gate.blog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.blog.domain.Blog;
import net.luversof.web.gate.blog.httpexchange.BlogClient;

@RestController
@RequestMapping(value = "/api/blog", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogController {

  private BlogClient blogClient;

  @Autowired
  public void setBlogClient(BlogClient blogClient) {
    this.blogClient = blogClient;
  }

  @BlueskyPreAuthorize
  @PostMapping
  public Blog create() {
    return blogClient.create(Blog.builder().userId(UserUtil.getUserId().toString()).build());
  }

  @GetMapping("/search/findByBlogId/{blogId}")
  public Optional<Blog> findByBlogId(@PathVariable String blogId) {
    return blogClient.findByBlogId(blogId);
  }

  @GetMapping("/search/findByUserId/{userId}")
  public List<Blog> findByUserId(@PathVariable String userId) {
    return blogClient.findByUserId(userId);
  }
}
