package net.luversof.api.bookkeeping.service;

import java.util.List;

import net.luversof.api.bookkeeping.domain.AssetGroup;

public interface AssetGroupService {

	List<AssetGroup> createInitialData(String bookkeepingId);
	
	AssetGroup create(AssetGroup assetGroup);
	
	List<AssetGroup> findByBookkeepingId(String bookkeepingId);
	
	AssetGroup update(AssetGroup assetGroup);
	
	void delete(AssetGroup assetGroup);
	
	void deleteAllByBookkeepingId(String bookkeepingId);

}
