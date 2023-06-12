package net.luversof.api.board.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.annotation.RoutingDataSource;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.repository.BoardRepository;

@Slf4j
//@Transactional(readOnly = true)
//@RoutingDataSource(value = "blog")
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
//	@RoutingDataSource("board")
	public Board findByAlias(String alias) {
		RoutingDataSourceContextHolder.setContext(() -> "board");
		var a = boardRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARD));
		log.debug("test : {}", a);
		RoutingDataSourceContextHolder.setContext(() -> "blog");
		var b = boardRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARD));
		log.debug("test22223333 : {}", b);
		return a;
	}
	
	public Board findByAlias2(String alias) {
		RoutingDataSourceContextHolder.setContext(() -> "blog");
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
