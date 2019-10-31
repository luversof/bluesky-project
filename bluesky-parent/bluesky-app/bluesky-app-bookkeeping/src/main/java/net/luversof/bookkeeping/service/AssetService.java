package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class AssetService {

	@Autowired
	private AssetRepository assetRepository;
	

	@Autowired
	private BookkeepingService bookkeepingService;
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<Asset> initialDataSave(Bookkeeping bookkeeping, List<AssetGroup> assetGroupList) {
		return assetRepository.saveAll(AssetInitialData.getAssetList(bookkeeping, assetGroupList));
	}
	
	public Asset create(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetRepository.save(asset);
	}
	
	public Asset update(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Asset targetAsset = findOne(asset.getId());
		if (!targetAsset.getBookkeeping().getUserId().equals(asset.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		return assetRepository.save(asset);
	}

	public Asset findOne(long id) {
		return assetRepository.getOne(id);
	}
	
	public List<Asset> getUserAssetList(UUID userId) {
		Bookkeeping userBookkeeping = bookkeepingService.getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetRepository.findByBookkeepingId(userBookkeeping.getId());
	}
	
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}
	
	public void delete(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Asset targetAsset = findOne(asset.getId());
		if (targetAsset.getBookkeeping().getUserId() != asset.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		assetRepository.delete(asset);
	}
	
	public void deleteBybookkeepingId(UUID bookkeepingId) {
		List<Asset> assetList = findByBookkeepingId(bookkeepingId);
		assetRepository.deleteAll(assetList);
	}
}
