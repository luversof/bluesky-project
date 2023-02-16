package net.luversof.web.gate.board.domain;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder(toBuilder = true)
public record BoardArticle(long id, String boardArticleId, String userId, String boardId, String title, String content, ZonedDateTime createdDate, ZonedDateTime lastModifiedDate) {

}
