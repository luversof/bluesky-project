package net.luversof.web.gate.board.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

/**
 * 게시글 정보를 나타내는 domain 클래스 (bluesky-api-board와 매핑)
 */
@Builder(toBuilder = true)
public record BoardArticle(UUID id, UUID userId, UUID boardId, String title, String content, Instant createdDate, Instant lastModifiedDate) {
}