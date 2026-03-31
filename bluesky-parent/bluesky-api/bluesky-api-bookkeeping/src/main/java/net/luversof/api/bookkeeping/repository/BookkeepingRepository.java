package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface BookkeepingRepository extends CrudRepository<Bookkeeping, UUID> {

    List<Bookkeeping> findByUserId(UUID userId);
}
