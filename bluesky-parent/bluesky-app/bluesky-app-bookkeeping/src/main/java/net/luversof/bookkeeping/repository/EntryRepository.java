package net.luversof.bookkeeping.repository;

import net.luversof.bookkeeping.domain.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	List<Entry> findByBookkeepingId(UUID bookkeepingId);
	
	List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "delete from Entry where bookkeeping_id = :bookkeepingId")
	int deleteByBookkeepingIdQuery(UUID bookkeepingId);
}
