package net.luversof.api.board.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.luversof.api.board.domain.BoardArticle;

@Repository
public interface BoardArticleRepository extends JpaRepository<BoardArticle, Long> {
	
	Optional<BoardArticle> findByBoardArticleId(String boardArticleId);
	
	Page<BoardArticle> findByBoardId(String boardId, Pageable pageable);
	
	void deleteByBoardArticleId(String boardArticleId);

}