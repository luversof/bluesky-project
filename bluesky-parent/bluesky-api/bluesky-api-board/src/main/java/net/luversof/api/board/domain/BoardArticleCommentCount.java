package net.luversof.api.board.domain;

import java.util.UUID;

import org.springframework.data.relational.core.mapping.Column;

public class BoardArticleCommentCount {

	@Column("boardArticle_id")
	private UUID boardArticleId;

	private long count;

	public BoardArticleCommentCount() {
	}

	public BoardArticleCommentCount(UUID boardArticleId, long count) {
		this.boardArticleId = boardArticleId;
		this.count = count;
	}

	public UUID getBoardArticleId() {
		return boardArticleId;
	}

	public void setBoardArticleId(UUID boardArticleId) {
		this.boardArticleId = boardArticleId;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "BoardArticleCommentCount[boardArticleId=" + boardArticleId + ", count=" + count + "]";
	}
}
