package net.luversof.api.bookkeeping.service;

import java.util.List;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import net.luversof.api.bookkeeping.domain.AssetGroup;

public interface AssetGroupService {

	List<AssetGroup> createInitialData(String bookkeepingId);
	
	AssetGroup create(@BlueskyValidated(AssetGroup.Create.class) AssetGroup assetGroup);
	
	List<AssetGroup> findByBookkeepingId(String bookkeepingId);
	
	AssetGroup update(@BlueskyValidated(AssetGroup.Update.class) AssetGroup assetGroup);
	
	void delete(@BlueskyValidated(AssetGroup.Delete.class) AssetGroup assetGroup);
	
	void deleteAllByBookkeepingId(String bookkeepingId);

}
