package net.luversof.web.gate.blog.controller;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import java.util.Optional;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.blog.domain.BlogArticle;
import net.luversof.web.gate.blog.httpexchange.BlogArticleClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/blogArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlogArticleController {

    @Autowired private BlogArticleClient blogArticleClient;

    @BlueskyPreAuthorize
    @PostMapping
    public BlogArticle create(@RequestBody BlogArticle blogArticle) {
        return blogArticleClient.create(
                blogArticle.toBuilder().userId(UserUtil.getUserId().toString()).build());
    }

    @GetMapping("/search/findByBlogId/{blogId}")
    public Page<BlogArticle> findByBlogId(@PathVariable String blogId, Pageable pageable) {
        return blogArticleClient.findByBlogId(blogId, pageable);
    }

    @GetMapping("/search/findByBlogArticleId/{blogArticleId}")
    public Optional<BlogArticle> findByBlogArticleId(@PathVariable String blogArticleId) {
        return blogArticleClient.findByBlogArticleId(blogArticleId);
    }

    @BlueskyPreAuthorize
    @PutMapping
    public BlogArticle update(@RequestBody BlogArticle blogArticle) {
        return blogArticleClient.update(
                blogArticle.toBuilder().userId(UserUtil.getUserId().toString()).build());
    }

    @BlueskyPreAuthorize
    @DeleteMapping
    public void delete(@RequestBody BlogArticle blogArticle) {
        blogArticleClient.delete(
                blogArticle.toBuilder().userId(UserUtil.getUserId().toString()).build());
    }
}
