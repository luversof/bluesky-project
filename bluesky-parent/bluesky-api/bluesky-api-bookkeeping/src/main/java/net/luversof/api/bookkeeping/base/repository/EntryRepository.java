package net.luversof.api.bookkeeping.base.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.Entry;

public interface EntryRepository extends JpaRepository<Entry, UUID> {
	
	List<Entry> findByBookkeepingId(UUID bookkeepingId);

}
