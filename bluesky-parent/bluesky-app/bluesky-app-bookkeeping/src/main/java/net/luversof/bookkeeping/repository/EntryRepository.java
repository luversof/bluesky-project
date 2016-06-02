package net.luversof.bookkeeping.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import net.luversof.bookkeeping.domain.Entry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, Long> {
	List<Entry> findByBookkeepingId(long bookkeepingId);
	List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, LocalDateTime startDate, LocalDateTime endDate);
	
	
	@Query(value = "SELECT entryGroup_id, SUM(amount) FROM Entry WHERE bookkeeping_id = 1 AND entryGroup_id IS NOT NULL GROUP BY entryGroup_id", nativeQuery = true)
	List<Map> test();
}
