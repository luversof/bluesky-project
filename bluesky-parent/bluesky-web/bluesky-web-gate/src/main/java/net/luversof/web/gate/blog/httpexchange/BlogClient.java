package net.luversof.web.gate.blog.httpexchange;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.blog.domain.Blog;

@HttpExchange(url = "/api/blog")
public interface BlogClient {

    @PostExchange
    Blog create(@RequestBody Blog blog);

    @GetExchange("/search/findByBlogId/{blogId}")
    Optional<Blog> findByBlogId(@PathVariable String blogId);

    @GetExchange("/search/findByUserId/{userId}")
    List<Blog> findByUserId(@PathVariable String userId);
}
