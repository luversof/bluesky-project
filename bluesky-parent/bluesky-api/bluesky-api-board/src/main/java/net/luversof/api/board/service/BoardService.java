package net.luversof.api.board.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.annotation.BlueskyRoutingDataSource;
import io.github.luversof.boot.jdbc.datasource.context.BlueskyRoutingDataSourceContextHolder;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.repository.BoardRepository;

@Transactional(readOnly = false)
@BlueskyRoutingDataSource("board")
@Service
public class BoardService {

	@Autowired
	private BoardRepository boardRepository;
	
	
	public Board create(Board board) {
		// TODO 기존에 있는지 확인
		
		
		if (!StringUtils.hasText(board.getBoardId())) {
			board.setBoardId(UUID.randomUUID().toString());
		}
		return boardRepository.save(board);
	}
	
//	@Transactional(readOnly = true)
//	@BlueskyRoutingDataSource("blog")
	public Board findByAlias(String alias) {
//		BlueskyRoutingDataSourceContextHolder.setContext(() -> "blog");
		return boardRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARD));
	}
	
	public Board update(Board board) {
		var targetBoard = findByAlias(board.getAlias());
		targetBoard.setBoardActivated(board.isBoardActivated());
		targetBoard.setArticleActivated(board.isArticleActivated());
		targetBoard.setReplyActivated(board.isReplyActivated());
		targetBoard.setCommentActivated(board.isCommentActivated());
		return boardRepository.save(targetBoard);
	}
}
