package net.luversof.api.board.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.Board;
import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.repository.BoardArticleRepository;


@Service
public class BoardArticleService {
	
	@Autowired
	private BoardService boardService;

	@Autowired
	private BoardArticleRepository boardArticleRepository;
	
	public BoardArticle create(BoardArticle boardArticle) {
		boardArticle.setBoardArticleId(UUID.randomUUID().toString());
		return boardArticleRepository.save(boardArticle);
	}
	
	public BoardArticle modify(BoardArticle boardArticle) {
		var targetBoardArticle = boardArticleRepository.findByBoardArticleId(boardArticle.getBoardArticleId()).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));

		if (!targetBoardArticle.getUserId().equals(boardArticle.getUserId())) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLE);
		}
		
		targetBoardArticle.setTitle(boardArticle.getTitle());
		targetBoardArticle.setContent(boardArticle.getContent());
		return boardArticleRepository.save(targetBoardArticle);
		
	}
	
	public Page<BoardArticle> findByAlias(String boardAlias, Pageable pageable) {
		Board board = boardService.findByAlias(boardAlias);
		return boardArticleRepository.findByBoardId(board.getBoardId(), pageable);
	}
	
	public Optional<BoardArticle> findByBoardArticleId(String bbsArticleId) {
		return boardArticleRepository.findByBoardArticleId(bbsArticleId);
	}

	// 삭제 처리는 어떻게? 
	public void deleteByBoardArticleId(String boardArticleId, String userId) {
		var targetBoardArticle = boardArticleRepository.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));
		
		if (!targetBoardArticle.getUserId().equals(userId)) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLE);
		}
		
		boardArticleRepository.delete(targetBoardArticle);
	}
	
}
