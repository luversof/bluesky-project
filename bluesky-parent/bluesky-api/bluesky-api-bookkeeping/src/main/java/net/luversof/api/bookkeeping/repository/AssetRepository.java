package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.domain.Asset;

@Transactional(readOnly = true)
public interface AssetRepository extends JpaRepository<Asset, Long> {
	
	List<Asset> findByBookkeepingId(String bookkeepingId);
	
	Optional<Asset> findByAssetId(String assetId);

}
