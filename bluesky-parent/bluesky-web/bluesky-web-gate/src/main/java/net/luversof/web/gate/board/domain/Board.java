package net.luversof.web.gate.board.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 게시판 정보를 나타내는 domain 클래스 (bluesky-api-board와 매핑)
 */
public record Board(UUID id, String alias, Map<String, Object> jsonConfig, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
}