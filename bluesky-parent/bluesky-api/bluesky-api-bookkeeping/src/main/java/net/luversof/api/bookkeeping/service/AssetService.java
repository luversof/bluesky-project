//package net.luversof.api.bookkeeping.service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import net.luversof.api.bookkeeping.domain.Asset;
//import net.luversof.api.bookkeeping.domain.AssetGroup;
//
//public interface AssetService {
//	
//	List<Asset> createInitialData(UUID bookkeepingId, List<AssetGroup> assetGroupList);
//
//	Asset create(Asset asset);
//	
//	Optional<Asset> findById(UUID assetId);
//	
//	List<Asset> findByBookkeepingId(UUID bookkeepingId);
//	
//	Asset update(Asset asset);
//	
//	void delete(Asset asset);
//	
//	void deleteAllByBookkeepingId(UUID bookkeepingId);
//}
