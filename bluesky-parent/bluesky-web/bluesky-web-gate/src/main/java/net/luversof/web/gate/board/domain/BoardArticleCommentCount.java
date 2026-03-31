package net.luversof.web.gate.board.domain;

import java.util.UUID;

public record BoardArticleCommentCount(UUID boardArticleId, long count) {}
