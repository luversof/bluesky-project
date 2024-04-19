//package net.luversof.api.bookkeeping.repository;
//
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import net.luversof.api.bookkeeping.base.domain.Entry;
//
////@Transactional(readOnly = true)
//public interface EntryRepository extends JpaRepository<Entry, UUID> {
//	
//	List<Entry> findByBookkeepingId(UUID bookkeepingId);
//	
//	List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate);
//	
////	@Modifying
////	@Transactional
////	@Query(nativeQuery = true, value = "delete from Entry where bookkeeping_id = :bookkeepingId")
//	void deleteByBookkeepingId(UUID bookkeepingId);
//}
