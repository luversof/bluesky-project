package net.luversof.api.board.service;

import io.github.luversof.boot.exception.BlueskyException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.luversof.api.board.constant.BoardErrorCode;
import net.luversof.api.board.domain.BoardArticleComment;
import net.luversof.api.board.domain.BoardArticleCommentCount;
import net.luversof.api.board.repository.BoardArticleCommentRepository;
import net.luversof.api.board.repository.BoardArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BoardArticleCommentService {

    @Autowired private BoardArticleRepository boardArticleRepository;

    @Autowired private BoardArticleCommentRepository boardArticleCommentRepository;

    public BoardArticleComment save(BoardArticleComment boardArticleComment) {
        boardArticleRepository
                .findById(boardArticleComment.getBoardArticleId())
                .orElseThrow(() -> new BlueskyException(BoardErrorCode.NOT_EXIST_BOARDARTICLE));
        return boardArticleCommentRepository.save(boardArticleComment);
    }

    public BoardArticleComment update(BoardArticleComment boardArticleComment) {
        var targetComment =
                boardArticleCommentRepository
                        .findById(boardArticleComment.getId())
                        .orElseThrow(
                                () ->
                                        new BlueskyException(
                                                BoardErrorCode.NOT_EXIST_BOARDARTICLECOMMENT));

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

    public List<BoardArticleCommentCount> countByBoardArticleIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return boardArticleCommentRepository.countByBoardArticleIds(ids);
    }

    public void delete(BoardArticleComment boardArticleComment) {
        var targetComment =
                boardArticleCommentRepository
                        .findById(boardArticleComment.getId())
                        .orElseThrow(
                                () ->
                                        new BlueskyException(
                                                BoardErrorCode.NOT_EXIST_BOARDARTICLECOMMENT));

        if (!targetComment.getUserId().equals(boardArticleComment.getUserId())) {
            throw new BlueskyException(BoardErrorCode.NOT_OWNER_BOARDARTICLECOMMENT);
        }

        boardArticleCommentRepository.delete(targetComment);
    }

    public void deleteByBoardArticleId(UUID boardArticleId) {
        boardArticleCommentRepository.deleteByBoardArticleId(boardArticleId);
    }
}
