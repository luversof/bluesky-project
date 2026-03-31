package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.EntryType;

public interface EntryTypeRepository extends CrudRepository<EntryType, UUID> {

    List<EntryType> findByBookkeepingId(UUID bookkeepingId);

    long deleteByBookkeepingId(UUID bookkeepingId);
}
