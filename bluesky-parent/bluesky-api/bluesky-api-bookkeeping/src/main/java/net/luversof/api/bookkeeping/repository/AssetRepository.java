package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.domain.Asset;

@Transactional(readOnly = true)
public interface AssetRepository extends JpaRepository<Asset, UUID> {
	
	List<Asset> findByBookkeepingId(UUID bookkeepingId);
	
}
