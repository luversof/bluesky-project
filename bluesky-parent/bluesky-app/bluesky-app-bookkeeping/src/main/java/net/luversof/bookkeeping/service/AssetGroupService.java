package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.AssetGroup;

public interface AssetGroupService {

	AssetGroup create(AssetGroup assetGroup);
	
	List<AssetGroup> findByBookkeepingId(String bookkeepingId);
	
	AssetGroup update(AssetGroup assetGroup);
	
	void delete(AssetGroup assetGroup);
	
	void deleteAllByBookkeepingId(AssetGroup assetGroup);

}
