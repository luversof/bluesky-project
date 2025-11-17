package net.luversof.web.gate.board.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record BoardArticleComment(
		UUID id,
		UUID boardArticleId,
		UUID userId,
		String username,
		String content,
		Instant createdDate,
		Instant lastModifiedDate) {
}
