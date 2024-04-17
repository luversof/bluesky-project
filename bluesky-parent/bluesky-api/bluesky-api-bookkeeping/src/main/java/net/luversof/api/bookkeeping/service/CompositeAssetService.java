package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.AssetGroup;

@Service
public class CompositeAssetService implements AssetService {
	
	@Autowired
	private BasicAssetService assetService;
	
//	@Autowired
//	private BasicAssetGroupService assetGroupService;
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;

	public List<Asset> createInitialData(UUID bookkeepingId, List<AssetGroup> assetGroupList) {
		return assetService.createInitialData(bookkeepingId, assetGroupList);
	}
	
	@Override
	public Asset create(Asset asset) {
		bookkeepingService.findById(asset.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING_ID));
		return assetService.create(asset);
	}
	
	@Override
	public Optional<Asset> findById(UUID assetId) {
		return assetService.findById(assetId);
	}
	
	@Override
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	@Override
	public Asset update(Asset asset) {
		Asset targetAsset = findById(asset.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
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
		bookkeepingService.findById(asset.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		assetService.delete(asset);
	}

	@Override
	public void deleteAllByBookkeepingId(UUID bookkeepingId) {
		assetService.deleteAllByBookkeepingId(bookkeepingId);
	}
	
}
