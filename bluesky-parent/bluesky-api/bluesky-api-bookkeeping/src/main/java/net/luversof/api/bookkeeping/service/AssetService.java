package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.AssetGroup;

public interface AssetService {
	
	List<Asset> createInitialData(String bookkeepingId, List<AssetGroup> assetGroupList);

	Asset create(@BlueskyValidated(Asset.Create.class) Asset asset);
	
	Optional<Asset> findByAssetId(String assetId);
	
	List<Asset> findByBookkeepingId(String bookkeepingId);
	
	Asset update(@BlueskyValidated(Asset.Update.class) Asset asset);
	
	void delete(@BlueskyValidated(Asset.Delete.class) Asset asset);
	
	void deleteAllByBookkeepingId(String bookkeepingId);
}
