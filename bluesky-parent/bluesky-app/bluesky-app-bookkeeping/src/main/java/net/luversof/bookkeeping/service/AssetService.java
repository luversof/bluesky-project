package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;

@Service
public class AssetService {

	@Autowired
	private AssetRepository assetRepository;
	
	@Autowired
	private AssetGroupService assetGroupService;
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<Asset> initialDataSave(Bookkeeping bookkeeping, List<AssetGroup> assetGroupList) {
		return assetRepository.saveAll(AssetInitialData.getAssetList(bookkeeping, assetGroupList));
	}
	
	public Asset createUserAsset(Asset asset) {
		asset.setBookkeeping(BookkeepingUtils.getBookkeepingService().getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING)));
		return assetRepository.save(asset);
	}
	
	public Optional<Asset> findById(long id) {
		return assetRepository.findById(id);
	}
	
	public List<Asset> getUserAssetList(UUID userId) {
		Bookkeeping userBookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetRepository.findByBookkeepingId(userBookkeeping.getId());
	}
	
	/**
	 * 변경할 컬럼을 일일이 지정하는 방식 말고 다른 방식은 없을까?
	 * @param asset
	 * @return
	 */
	public Asset updateUserAsset(Asset asset) {
		Bookkeeping bookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		//asset.setBookkeeping();
		Asset targetAsset = findById(asset.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
		if (!targetAsset.getBookkeeping().getUserId().equals(bookkeeping.getUserId())) {
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
		
		return assetRepository.save(targetAsset);
	}

	public void deleteUserAsset(Asset asset) {
		BookkeepingUtils.getBookkeepingService().getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Asset targetAsset = findById(asset.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
		if (!targetAsset.getBookkeeping().getUserId().equals(asset.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		assetRepository.delete(targetAsset);
	}

	public void deleteBybookkeepingId(UUID bookkeepingId) {
		List<Asset> assetList = assetRepository.findByBookkeepingId(bookkeepingId);
		assetRepository.deleteAll(assetList);
	}
}
