package net.luversof.api.board.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.repository.BoardRepository;

@Service
public class BoardService {

	@Setter(onMethod_ = @Autowired)
	private BoardRepository boardRepository;
	
	public Page<Board> findAll(Pageable pageable) {
		return boardRepository.findAll(pageable);
	}
	
	public Board create(Board board) {
		return boardRepository.save(board);
	}
	
	public Board findByAlias(String alias) {
		return boardRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARD));
	}
	
	public Board update(Board board) {
		return boardRepository.save(board);
	}
}
