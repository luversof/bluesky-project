//package net.luversof.api.bookkeeping.repository;
//
//import java.util.List;
//import java.util.UUID;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.transaction.annotation.Transactional;
//
//import net.luversof.api.bookkeeping.domain.AssetGroup;
//
//
//@Transactional(readOnly = true)
//public interface AssetGroupRepository extends JpaRepository<AssetGroup, UUID> {
//	
//	List<AssetGroup> findByBookkeepingId(UUID bookkeepingId);
//
//}