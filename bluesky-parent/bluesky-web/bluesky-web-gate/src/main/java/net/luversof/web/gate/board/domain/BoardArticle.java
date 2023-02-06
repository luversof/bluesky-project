package net.luversof.web.gate.board.domain;

import java.time.LocalDateTime;

public record BoardArticle(long id, String boardArticleId, String userId, String title, String content, LocalDateTime createdDate, LocalDateTime lastModifiedDate) {

}
