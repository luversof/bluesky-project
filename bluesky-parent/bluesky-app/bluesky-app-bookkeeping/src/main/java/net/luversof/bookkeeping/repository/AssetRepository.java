package net.luversof.bookkeeping.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Asset;

@Transactional(readOnly = true)
public interface AssetRepository extends JpaRepository<Asset, Long> {
	
	List<Asset> findByBookkeepingId(String bookkeepingId);
}
