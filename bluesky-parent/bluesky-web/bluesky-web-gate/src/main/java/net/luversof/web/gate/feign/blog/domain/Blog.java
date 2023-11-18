package net.luversof.web.gate.feign.blog.domain;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Builder;

@Builder(toBuilder = true)
public record Blog(long idx, String blogId, String userId, List<BlogArticleCategory> blogArticleCategoryList, ZonedDateTime createdDate) {

}
