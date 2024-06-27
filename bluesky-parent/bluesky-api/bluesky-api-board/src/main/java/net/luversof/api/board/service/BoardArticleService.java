package net.luversof.api.board.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.repository.BoardArticleRepository;


@Service
public class BoardArticleService {
	
	@Setter(onMethod_ = @Autowired)
	private BoardService boardService;

	@Setter(onMethod_ = @Autowired)
	private BoardArticleRepository boardArticleRepository;
	
	public BoardArticle create(BoardArticle boardArticle) {
		return boardArticleRepository.save(boardArticle);
	}
	
	public BoardArticle modify(BoardArticle boardArticle) {
		var targetBoardArticle = boardArticleRepository.findById(boardArticle.getId()).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));

		if (!targetBoardArticle.getUserId().equals(boardArticle.getUserId())) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLE);
		}
		
		targetBoardArticle.setTitle(boardArticle.getTitle());
		targetBoardArticle.setContent(boardArticle.getContent());
		return boardArticleRepository.save(targetBoardArticle);
		
	}
	
	public Page<BoardArticle> findByAlias(String boardAlias, Pageable pageable) {
		Board board = boardService.findByAlias(boardAlias);
		return boardArticleRepository.findByBoardId(board.getId(), pageable);
	}
	
	public Optional<BoardArticle> findById(UUID id) {
		return boardArticleRepository.findById(id);
	}

	// 삭제 처리는 어떻게? 
	public void delete(BoardArticle boardArticle) {
		var targetBoardArticle = boardArticleRepository.findById(boardArticle.getId()).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));
		
		if (!targetBoardArticle.getUserId().equals(boardArticle.getUserId())) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLE);
		}
		
		boardArticleRepository.delete(targetBoardArticle);
	}
	
}
