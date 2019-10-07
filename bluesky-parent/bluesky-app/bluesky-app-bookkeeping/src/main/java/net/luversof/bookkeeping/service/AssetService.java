package net.luversof.bookkeeping.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.core.exception.BlueskyException;

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
	public List<Asset> initialDataSave(Bookkeeping bookkeeping) {
		Set<Asset> assetSet = new HashSet<>();
		for (AssetInitialData assetInitialData : AssetInitialData.values()) {
			Asset asset = new Asset();
			asset.setBookkeeping(bookkeeping);
			asset.setAmount(0);
			asset.setAssetType(assetInitialData.getAssetType());
			asset.setName(assetInitialData.getName());
			assetSet.add(asset);
		}
		return assetRepository.saveAll(assetSet);
	}
	
	public Asset create(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetRepository.save(asset);
	}
	
	public Asset update(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		Asset targetAsset = findOne(asset.getId());
		if (!targetAsset.getBookkeeping().getUserId().equals(asset.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		return assetRepository.save(asset);
	}

	public Asset findOne(long id) {
		return assetRepository.getOne(id);
	}
	
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}
	
	public void delete(Asset asset) {
		bookkeepingService.getUserBookkeeping(asset.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
