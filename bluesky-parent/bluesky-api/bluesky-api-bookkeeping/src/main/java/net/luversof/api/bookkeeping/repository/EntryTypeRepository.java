package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;
import net.luversof.api.bookkeeping.domain.EntryType;
import org.springframework.data.repository.CrudRepository;

public interface EntryTypeRepository extends CrudRepository<EntryType, UUID> {

    List<EntryType> findByBookkeepingId(UUID bookkeepingId);

    long deleteByBookkeepingId(UUID bookkeepingId);
}
