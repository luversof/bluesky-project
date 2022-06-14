package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;

public class CompositeAssetService implements AssetService {
	
	@Autowired
	private BasicAssetService assetService;
	
	@Autowired
	private BasicAssetGroupService assetGroupService;

	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<Asset> initialDataSave(Bookkeeping bookkeeping, List<AssetGroup> assetGroupList) {
		return assetRepository.saveAll(AssetInitialData.getAssetList(bookkeeping, assetGroupList));
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
		
		if (asset.getAssetGroup().getId() != targetAsset.getAssetGroup().getId()) {
			List<AssetGroup> userAssetGroupList = assetGroupService.getUserAssetGroupList(asset.getBookkeeping().getUserId());
			userAssetGroupList.forEach(userAssetGroup -> {
				if (userAssetGroup.getId() == asset.getAssetGroup().getId() && userAssetGroup.getAssetGroupType() != null) {
					throw new BlueskyException(BookkeepingErrorCode.DISABLED_CHANGE_ASSET_GROUP);
				} else {
					targetAsset.setAssetGroup(userAssetGroup);
				}
			});
		}
		targetAsset.setName(asset.getName());
		
		return assetService.update(targetAsset);
	}
	
	@Override
	public void delete(Asset asset) {
		BookkeepingUtils.getBookkeepingService().getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		assetService.delete(asset);
	}

	@Override
	public void deleteAllBybookkeepingId(Asset asset) {
		assetService.deleteAllBybookkeepingId(asset);
	}
	
}
