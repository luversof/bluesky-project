package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;

@Service
public class CompositeAssetService implements AssetService {
	
	@Autowired
	private BasicAssetService assetService;
	
//	@Autowired
//	private BasicAssetGroupService assetGroupService;
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;

	public List<Asset> createInitialData(String bookkeepingId, List<AssetGroup> assetGroupList) {
		return assetService.createInitialData(bookkeepingId, assetGroupList);
	}
	
	@Override
	public Asset create(Asset asset) {
		return assetService.create(asset);
	}
	
	@Override
	public Optional<Asset> findByAssetId(String assetId) {
		return assetService.findByAssetId(assetId);
	}
	
	@Override
	public List<Asset> findByBookkeepingId(String bookkeepingId) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	@Override
	public Asset update(Asset asset) {
		Asset targetAsset = findByAssetId(asset.getAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		
//		if (asset.getAssetGroup().getId() != targetAsset.getAssetGroup().getId()) {
//			List<AssetGroup> userAssetGroupList = assetGroupService.getUserAssetGroupList(asset.getBookkeeping().getUserId());
//			userAssetGroupList.forEach(userAssetGroup -> {
//				if (userAssetGroup.getId() == asset.getAssetGroup().getId() && userAssetGroup.getAssetGroupType() != null) {
//					throw new BlueskyException(BookkeepingErrorCode.DISABLED_CHANGE_ASSET_GROUP);
//				} else {
//					targetAsset.setAssetGroup(userAssetGroup);
//				}
//			});
//		}
		targetAsset.setName(asset.getName());
		
		return assetService.update(targetAsset);
	}
	
	@Override
	public void delete(Asset asset) {
		bookkeepingService.findByBookkeepingId(asset.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		assetService.delete(asset);
	}

	@Override
	public void deleteAllByBookkeepingId(String bookkeepingId) {
		assetService.deleteAllByBookkeepingId(bookkeepingId);
	}
	
}
