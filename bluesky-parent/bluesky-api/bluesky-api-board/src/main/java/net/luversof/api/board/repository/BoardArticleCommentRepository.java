package net.luversof.api.board.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import net.luversof.api.board.domain.BoardArticleComment;

public interface BoardArticleCommentRepository
		extends PagingAndSortingRepository<BoardArticleComment, UUID>, CrudRepository<BoardArticleComment, UUID> {

	Page<BoardArticleComment> findByBoardArticleId(UUID boardArticleId, Pageable pageable);

	long countByBoardArticleId(UUID boardArticleId);

	void deleteByBoardArticleId(UUID boardArticleId);
}
