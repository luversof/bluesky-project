package net.luversof.web.gate.blog.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record BlogArticleCategory(long idx, String blogArticleCategoryId, String blogId, String name) {

}
