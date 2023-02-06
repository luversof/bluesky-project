package net.luversof.web.gate.board.domain;

public record Board(long id, String boardId, String alias, boolean isBoardActivated, boolean isArticleActivated, boolean isReplyActivated, boolean isCommentActivated) {

}
