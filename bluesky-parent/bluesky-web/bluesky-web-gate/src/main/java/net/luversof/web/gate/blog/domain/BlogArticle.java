package net.luversof.web.gate.blog.domain;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Builder;

@Builder(toBuilder = true)
public record BlogArticle(long idx, String blogArticleId, String blogId, String userId, BlogArticleCategory blogArticleCategory, List<BlogArticleComment> blogArticleCommentList, String title, String content, ZonedDateTime createdDate, ZonedDateTime lastModifiedDate) {

}
