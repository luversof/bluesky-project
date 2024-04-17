package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import net.luversof.api.bookkeeping.domain.AssetGroup;

public interface AssetGroupService {

	List<AssetGroup> createInitialData(UUID bookkeepingId);
	
	AssetGroup create(AssetGroup assetGroup);
	
	List<AssetGroup> findByBookkeepingId(UUID bookkeepingId);
	
	AssetGroup update(AssetGroup assetGroup);
	
	void delete(AssetGroup assetGroup);
	
	void deleteAllByBookkeepingId(UUID bookkeepingId);

}
