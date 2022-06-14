package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;

@Service
public class BasicAssetService implements AssetService {

	@Autowired
	private AssetRepository assetRepository;
	
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
		return assetRepository.save(asset);
	}
	
	@Override
	public Optional<Asset> findByAssetId(String assetId) {
		return assetRepository.findByAssetId(assetId);
	}
	
	@Override
	public List<Asset> findByBookkeepingId(String bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}

	@Override
	public Asset update(Asset asset) {
		Asset targetAsset = findByAssetId(asset.getAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		targetAsset.setName(asset.getName());
		
		return assetRepository.save(targetAsset);
	}
	
	@Override
	public void delete(Asset asset) {
		Asset targetAsset = findByAssetId(asset.getAssetId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSET));
		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSET);
		}
		assetRepository.delete(targetAsset);
	}

	@Override
	public void deleteAllBybookkeepingId(Asset asset) {
		List<Asset> assetList = assetRepository.findByBookkeepingId(asset.getBookkeepingId());
		assetRepository.deleteAll(assetList);
	}
}
