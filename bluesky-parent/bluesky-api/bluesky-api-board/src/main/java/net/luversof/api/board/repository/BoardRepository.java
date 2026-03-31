package net.luversof.api.board.repository;

import java.util.Optional;
import java.util.UUID;
import net.luversof.api.board.domain.Board;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface BoardRepository extends CrudRepository<Board, UUID> {

    Optional<Board> findByAlias(String alias);
}
