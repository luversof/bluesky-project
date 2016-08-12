package net.luversof.bookkeeping.repository;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Entry;

@Transactional(readOnly = true)
public interface EntryRepository extends JpaRepository<Entry, Long> {
	
	List<Entry> findByBookkeepingId(long bookkeepingId);
	
	List<Entry> findByBookkeepingIdAndEntryDateBetween(long bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
	
	/**
	 * 테스트용
	 * @param id
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "delete from Entry where id = :id")
	void deleteById(@Param("id") long id);
}
