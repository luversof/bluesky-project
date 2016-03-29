package net.luversof.bookkeeping.repository;

import java.time.LocalDate;
import java.util.List;

import net.luversof.bookkeeping.domain.Entry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
	List<Entry> findByBookkeepingId(long bookkeepingId);
	List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, LocalDate startDate, LocalDate endDate);
}
