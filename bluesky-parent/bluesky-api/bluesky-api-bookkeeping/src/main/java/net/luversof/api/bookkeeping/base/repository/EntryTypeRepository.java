package net.luversof.api.bookkeeping.base.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.EntryType;

public interface EntryTypeRepository extends JpaRepository<EntryType, UUID> {

	List<EntryType> findByBookkeepingId(UUID bookkeepingId);

}
