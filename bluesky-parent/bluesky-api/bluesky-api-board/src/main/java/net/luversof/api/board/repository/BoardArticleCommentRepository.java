package net.luversof.api.board.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import net.luversof.api.board.domain.BoardArticleComment;
import net.luversof.api.board.domain.BoardArticleCommentCount;

public interface BoardArticleCommentRepository
    extends PagingAndSortingRepository<BoardArticleComment, UUID>,
        CrudRepository<BoardArticleComment, UUID> {

  Page<BoardArticleComment> findByBoardArticleId(UUID boardArticleId, Pageable pageable);

  long countByBoardArticleId(UUID boardArticleId);

  void deleteByBoardArticleId(UUID boardArticleId);

  // batch count by board article ids
  @Query(
      """
			SELECT "boardArticle_id", COUNT(*) AS count
			FROM "BoardArticleComment"
			WHERE "boardArticle_id" IN (:ids)
			GROUP BY "boardArticle_id"
		""")
  List<BoardArticleCommentCount> countByBoardArticleIds(@Param("ids") Collection<UUID> ids);
}
