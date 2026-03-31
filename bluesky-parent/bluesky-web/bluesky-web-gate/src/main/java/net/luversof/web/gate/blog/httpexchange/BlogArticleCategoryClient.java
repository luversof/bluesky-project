package net.luversof.web.gate.blog.httpexchange;

import java.util.List;
import net.luversof.web.gate.blog.domain.BlogArticleCategory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/api/blogArticleCategory", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BlogArticleCategoryClient {

    @PostExchange
    BlogArticleCategory create(@RequestBody BlogArticleCategory blogArticleCategory);

    @GetExchange("/search/findByBlogId/{blogId}")
    List<BlogArticleCategory> findByBlogId(@PathVariable String blogId);

    @PutMapping
    BlogArticleCategory update(@RequestBody BlogArticleCategory blogArticleCategory);

    @DeleteMapping
    void delete(@RequestBody BlogArticleCategory blogArticleCategory);
}
