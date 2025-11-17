package net.luversof.api.board.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.BoardArticleComment;
import net.luversof.api.board.repository.BoardArticleCommentRepository;
import net.luversof.api.board.repository.BoardArticleRepository;

@Service
public class BoardArticleCommentService {

	@Setter(onMethod_ = @Autowired)
	private BoardArticleRepository boardArticleRepository;

	@Setter(onMethod_ = @Autowired)
	private BoardArticleCommentRepository boardArticleCommentRepository;

	public BoardArticleComment save(BoardArticleComment boardArticleComment) {
		boardArticleRepository.findById(boardArticleComment.getBoardArticleId())
				.orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));
		return boardArticleCommentRepository.save(boardArticleComment);
	}

	public BoardArticleComment update(BoardArticleComment boardArticleComment) {
		var targetComment = boardArticleCommentRepository.findById(boardArticleComment.getId())
				.orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLECOMMENT));

		if (!targetComment.getUserId().equals(boardArticleComment.getUserId())) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLECOMMENT);
		}

		targetComment.setContent(boardArticleComment.getContent());
		return boardArticleCommentRepository.save(targetComment);
	}

	public Page<BoardArticleComment> findByBoardArticleId(UUID boardArticleId, Pageable pageable) {
		return boardArticleCommentRepository.findByBoardArticleId(boardArticleId, pageable);
	}

	public long countByBoardArticleId(UUID boardArticleId) {
		return boardArticleCommentRepository.countByBoardArticleId(boardArticleId);
	}

	public void delete(BoardArticleComment boardArticleComment) {
		var targetComment = boardArticleCommentRepository.findById(boardArticleComment.getId())
				.orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLECOMMENT));

		if (!targetComment.getUserId().equals(boardArticleComment.getUserId())) {
			throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLECOMMENT);
		}

		boardArticleCommentRepository.delete(targetComment);
	}

	public void deleteByBoardArticleId(UUID boardArticleId) {
		boardArticleCommentRepository.deleteByBoardArticleId(boardArticleId);
	}
}
