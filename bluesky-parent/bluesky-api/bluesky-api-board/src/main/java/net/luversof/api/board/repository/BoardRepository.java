package net.luversof.api.board.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.board.domain.Board;

@Transactional(readOnly = true)
public interface BoardRepository extends JpaRepository<Board, Long> {
	Optional<Board> findByAlias(String alias);
}
