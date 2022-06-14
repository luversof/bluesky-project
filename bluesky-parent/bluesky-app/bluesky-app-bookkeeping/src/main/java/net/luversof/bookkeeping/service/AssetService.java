package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import net.luversof.bookkeeping.domain.Asset;

public interface AssetService {

	Asset create(Asset asset);
	
	Optional<Asset> findByAssetId(String assetId);
	
	List<Asset> findByBookkeepingId(String bookkeepingId);
	
	Asset update(Asset asset);
	
	void delete(Asset asset);
	
	void deleteAllBybookkeepingId(Asset asset);
}
