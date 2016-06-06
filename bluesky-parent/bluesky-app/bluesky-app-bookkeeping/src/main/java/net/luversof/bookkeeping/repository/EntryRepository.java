package net.luversof.bookkeeping.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.Statistics;

public interface EntryRepository extends JpaRepository<Entry, Long> {
	List<Entry> findByBookkeepingId(long bookkeepingId);
	List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, LocalDateTime startDate, LocalDateTime endDate);
}
