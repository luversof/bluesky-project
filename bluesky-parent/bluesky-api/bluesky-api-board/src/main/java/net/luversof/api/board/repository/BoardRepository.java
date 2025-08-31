package net.luversof.api.board.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.board.domain.Board;

@Transactional(readOnly = true)
public interface BoardRepository extends CrudRepository<Board, UUID> {
	
	Optional<Board> findByAlias(String alias);

}
