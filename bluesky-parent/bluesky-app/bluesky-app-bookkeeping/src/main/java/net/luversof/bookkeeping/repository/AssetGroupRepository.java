package net.luversof.bookkeeping.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.AssetGroup;

@Transactional(readOnly = true)
public interface AssetGroupRepository extends JpaRepository<AssetGroup, Long> {
	
	List<AssetGroup> findByBookkeepingId(String bookkeepingId);

}
