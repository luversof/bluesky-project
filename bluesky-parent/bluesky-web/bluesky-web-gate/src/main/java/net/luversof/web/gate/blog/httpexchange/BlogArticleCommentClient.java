package net.luversof.web.gate.blog.httpexchange;

import java.util.Optional;
import net.luversof.web.gate.blog.domain.BlogArticleComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/api/blogArticleComment")
public interface BlogArticleCommentClient {

    @PostExchange
    BlogArticleComment create(@RequestBody BlogArticleComment blogArticleComment);

    @GetExchange("/search/findByBlogArticleId/{blogArticleId}")
    Page<BlogArticleComment> findByBlogArticleId(
            @PathVariable String blogArticleId, Pageable pageable);

    @GetExchange("/search/findByBlogArticleCommentId/{blogArticleCommentId}")
    Optional<BlogArticleComment> findByBlogArticleCommentId(
            @PathVariable String blogArticleCommentId);

    @GetExchange("/search/countByBlogArticleId/{blogArticleId}")
    long countByBlogArticleId(@PathVariable String blogArticleId);

    @PutMapping
    BlogArticleComment update(@RequestBody BlogArticleComment blogArticleComment);

    @DeleteMapping
    void delete(@RequestBody BlogArticleComment blogArticleComment);
}
