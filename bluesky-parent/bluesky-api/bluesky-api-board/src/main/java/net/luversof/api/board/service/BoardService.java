package net.luversof.api.board.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.repository.BoardRepository;

@Service
public class BoardService {

	@Autowired
	private BoardRepository boardRepository;

	public Iterable<Board> findAll() {
		return boardRepository.findAll();
	}

	public Board create(Board board) {
		return boardRepository.save(board);
	}

	public Board findByAlias(String alias) {
		return boardRepository.findByAlias(alias)
				.orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARD));
	}

	public Board update(Board board) {
		return boardRepository.save(board);
	}
}
