package net.luversof.api.board.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.board.domain.BoardArticle;

public interface BoardArticleRepository extends CrudRepository<BoardArticle, UUID> {

  Page<BoardArticle> findByBoardId(UUID boardId, Pageable pageable);
}
