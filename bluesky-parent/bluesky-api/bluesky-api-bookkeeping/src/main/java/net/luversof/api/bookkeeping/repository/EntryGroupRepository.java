//package net.luversof.api.bookkeeping.repository;
//
//import java.util.List;
//import java.util.UUID;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.transaction.annotation.Transactional;
//
//import net.luversof.api.bookkeeping.domain.EntryGroup;
//
//@Transactional(readOnly = true)
//public interface EntryGroupRepository extends JpaRepository<EntryGroup, UUID> {
//
//	List<EntryGroup> findByBookkeepingId(UUID bookkeepingId);
//	
//}