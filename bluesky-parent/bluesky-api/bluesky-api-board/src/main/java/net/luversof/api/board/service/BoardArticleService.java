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
import net.luversof.api.board.repository.BoardArticleCommentRepository;
import net.luversof.api.board.repository.BoardArticleRepository;

@Service
public class BoardArticleService {

    @Autowired private BoardService boardService;

    @Autowired private BoardArticleRepository boardArticleRepository;

    @Autowired private BoardArticleCommentRepository boardArticleCommentRepository;

    public BoardArticle save(BoardArticle boardArticle) {
        return boardArticleRepository.save(boardArticle);
    }

    public BoardArticle update(BoardArticle boardArticle) {
        var targetBoardArticle =
                boardArticleRepository
                        .findById(boardArticle.getId())
                        .orElseThrow(
                                () -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));

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

    public void delete(BoardArticle boardArticle) {
        var targetBoardArticle =
                boardArticleRepository
                        .findById(boardArticle.getId())
                        .orElseThrow(
                                () -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));

        if (!targetBoardArticle.getUserId().equals(boardArticle.getUserId())) {
            throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLE);
        }

        boardArticleCommentRepository.deleteByBoardArticleId(targetBoardArticle.getId());
        boardArticleRepository.delete(targetBoardArticle);
    }
}
