package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.AssetGroup;

public interface AssetService {
	
	List<Asset> createInitialData(String bookkeepingId, List<AssetGroup> assetGroupList);

	Asset create(Asset asset);
	
	Optional<Asset> findByAssetId(String assetId);
	
	List<Asset> findByBookkeepingId(String bookkeepingId);
	
	Asset update(Asset asset);
	
	void delete(Asset asset);
	
	void deleteAllByBookkeepingId(String bookkeepingId);
}
