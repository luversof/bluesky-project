//package net.luversof.api.bookkeeping.service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.constant.AssetInitialData;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//import net.luversof.api.bookkeeping.domain.Asset;
//import net.luversof.api.bookkeeping.domain.AssetGroup;
//import net.luversof.api.bookkeeping.repository.AssetRepository;
//
//@Service
//public class BasicAssetService implements AssetService {
//
//	@Autowired
//	private AssetRepository assetRepository;
//	
//	/**
//	 * 초기 데이터 insert
//	 * @param bookkeeping
//	 * @return
//	 */
//	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
//	public List<Asset> createInitialData(UUID bookkeepingId, List<AssetGroup> assetGroupList) {
//		return assetRepository.saveAll(AssetInitialData.createAssetList(bookkeepingId, assetGroupList));
//	}
//	
//	@Override
//	public Asset create(Asset asset) {
//		return assetRepository.save(asset);
//	}
//	
//	@Override
//	public Optional<Asset> findById(UUID assetId) {
//		return assetRepository.findById(assetId);
//	}
//	
//	@Override
//	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
//		return assetRepository.findByBookkeepingId(bookkeepingId);
//	}
//
//	@Override
//	public Asset update(Asset asset) {
//		Asset targetAsset = findById(asset.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
//		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
//			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
//		}
//		targetAsset.setName(asset.getName());
//		
//		return assetRepository.save(targetAsset);
//	}
//	
//	@Override
//	public void delete(Asset asset) {
//		Asset targetAsset = findById(asset.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
//		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
//			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
//		}
//		assetRepository.delete(targetAsset);
//	}
//
//	@Override
//	public void deleteAllByBookkeepingId(UUID bookkeepingId) {
//		List<Asset> assetList = assetRepository.findByBookkeepingId(bookkeepingId);
//		assetRepository.deleteAll(assetList);
//	}
//}
