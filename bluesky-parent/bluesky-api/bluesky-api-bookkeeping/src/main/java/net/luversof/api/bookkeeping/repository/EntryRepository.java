package net.luversof.api.bookkeeping.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.domain.Entry;

//@Transactional(readOnly = true)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	Optional<Entry> findByEntryId(String entryId);
	
	List<Entry> findByBookkeepingId(String bookkeepingId);
	
	List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
	
//	@Modifying
//	@Transactional
//	@Query(nativeQuery = true, value = "delete from Entry where bookkeeping_id = :bookkeepingId")
	void deleteByBookkeepingId(String bookkeepingId);
}
