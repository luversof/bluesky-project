package net.luversof.bookkeeping.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.bookkeeping.domain.Entry;

//@Transactional(readOnly = true)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	Optional<Entry> findByEntryId(String entryId);
	
	List<Entry> findByBookkeepingId(String bookkeepingId);
	
	List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate);
	
//	@Modifying
//	@Transactional
//	@Query(nativeQuery = true, value = "delete from Entry where bookkeeping_id = :bookkeepingId")
	void deleteByBookkeepingId(String bookkeepingId);
}
