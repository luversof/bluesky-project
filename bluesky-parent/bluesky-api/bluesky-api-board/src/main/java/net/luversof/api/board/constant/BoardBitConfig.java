package net.luversof.api.board.constant;

import static net.luversof.api.board.constant.BoardAction.CREATE;
import static net.luversof.api.board.constant.BoardAction.DELETE;
import static net.luversof.api.board.constant.BoardAction.READ;
import static net.luversof.api.board.constant.BoardAction.UPDATE;
import static net.luversof.api.board.constant.BoardActionGroup.ARTICLE;
import static net.luversof.api.board.constant.BoardActionGroup.COMMENT;
import static net.luversof.api.board.constant.BoardActionGroup.REPLY;
import static net.luversof.api.board.constant.BoardRole.ADMIN;
import static net.luversof.api.board.constant.BoardRole.USER;

import java.util.BitSet;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Board 설정값
 * 유저 별로 다른 권한은 어떻게 처리해야 할까?
 * 아래 처럼 하면 Role 별로 전부 정의해야 하는 문제가 있는데 더 좋은 방법은 없을까?
 */
@Getter
@AllArgsConstructor
public enum BoardBitConfig {
	
	ENABLE_BOARD(0, true, null, null, null),
	
	ENABLE_ADMIN_ARTICLE_CREATE(101, true, ADMIN, ARTICLE, CREATE),
	ENABLE_ADMIN_ARTICLE_READ(102, true, ADMIN, ARTICLE, READ),
	ENABLE_ADMIN_ARTICLE_UPDATE(103, true, ADMIN, ARTICLE, UPDATE),
	ENABLE_ADMIN_ARTICLE_DELETE(104, true, ADMIN, ARTICLE, DELETE),
	ENABLE_ADMIN_REPLY_CREATE(105, true, ADMIN, REPLY, CREATE),
	ENABLE_ADMIN_REPLY_READ(106, true, ADMIN, REPLY, READ),
	ENABLE_ADMIN_REPLY_UPDATE(107, true, ADMIN, REPLY, UPDATE),
	ENABLE_ADMIN_REPLY_DELETE(108, true, ADMIN, REPLY, DELETE),
	ENABLE_ADMIN_COMMENT_CREATE(109, true, ADMIN, COMMENT, CREATE),
	ENABLE_ADMIN_COMMENT_READ(110, true, ADMIN, COMMENT, READ),
	ENABLE_ADMIN_COMMENT_UPDATE(111, true, ADMIN, COMMENT, UPDATE),
	ENABLE_ADMIN_COMMENT_DELETE(112, true, ADMIN, COMMENT, DELETE),
	ENABLE_USER_ARTICLE_CREATE(201, true, USER, ARTICLE, CREATE),
	ENABLE_USER_ARTICLE_READ(202, true, USER, ARTICLE, READ),
	ENABLE_USER_ARTICLE_UPDATE(203, true, USER, ARTICLE, UPDATE),
	ENABLE_USER_ARTICLE_DELETE(204, true, USER, ARTICLE, DELETE),
	ENABLE_USER_REPLY_CREATE(205, true, USER, REPLY, CREATE),
	ENABLE_USER_REPLY_READ(206, true, USER, REPLY, READ),
	ENABLE_USER_REPLY_UPDATE(207, true, USER, REPLY, UPDATE),
	ENABLE_USER_REPLY_DELETE(208, true, USER, REPLY, DELETE),
	ENABLE_USER_COMMENT_CREATE(209, true, USER, COMMENT, CREATE),
	ENABLE_USER_COMMENT_READ(210, true, USER, COMMENT, READ),
	ENABLE_USER_COMMENT_UPDATE(211, true, USER, COMMENT, UPDATE),
	ENABLE_USER_COMMENT_DELETE(212, true, USER, COMMENT, DELETE),
	;
	
	private int bitIndex;
	private boolean value;
	private BoardRole role;
	private BoardActionGroup actionGroup;
	private BoardAction action;
	
	public static BitSet getBitConfig() {
		var bitSet =  new BitSet();
		for (var boardBitConfig : BoardBitConfig.values()) {
			bitSet.set(boardBitConfig.getBitIndex(), boardBitConfig.isValue());
		}
		return bitSet;
	}
	
}
