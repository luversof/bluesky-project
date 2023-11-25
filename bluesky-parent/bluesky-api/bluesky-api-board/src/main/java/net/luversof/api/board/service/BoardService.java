package net.luversof.api.board.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.repository.BoardRepository;

@Service
public class BoardService {

	@Autowired
	private BoardRepository boardRepository;
	
	public Page<Board> findAll(Pageable pageable) {
		return boardRepository.findAll(pageable);
	}
	
	public Board create(Board board) {
		if (!StringUtils.hasText(board.getBoardId())) {
			board.setBoardId(UUID.randomUUID().toString());
		}
		return boardRepository.save(board);
	}
	
	public Board findByAlias(String alias) {
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
