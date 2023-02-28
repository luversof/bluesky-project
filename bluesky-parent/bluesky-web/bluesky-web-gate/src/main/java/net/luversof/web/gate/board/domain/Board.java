package net.luversof.web.gate.board.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record Board(long id, String boardId, String alias, boolean isBoardActivated, boolean isArticleActivated, boolean isReplyActivated, boolean isCommentActivated) {

}
