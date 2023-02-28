package net.luversof.web.gate.blog.domain;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder(toBuilder = true)
public record BlogArticleComment(long idx, String blogArticleCommentId, String blogArticleId, String userId, String comment, ZonedDateTime createdDate, ZonedDateTime lastModifiedDate) {

}
