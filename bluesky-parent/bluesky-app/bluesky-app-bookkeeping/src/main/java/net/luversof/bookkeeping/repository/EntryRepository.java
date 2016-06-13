package net.luversof.bookkeeping.repository;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Entry;

@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	List<Entry> findByBookkeepingId(long bookkeepingId);
	
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, LocalDateTime startDate, LocalDateTime endDate);
}
