package net.luversof.bookkeeping.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Entry;

@Transactional(readOnly = true)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	List<Entry> findByBookkeepingId(UUID bookkeepingId);
	
	List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "delete from Entry where bookkeeping_id = :bookkeepingId")
	int deleteByBookkeepingIdQuery(UUID bookkeepingId);
}
