package net.luversof.bookkeeping.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional("bookkeepingTransactionManager")
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
	public List<Asset> initialDataSave(Bookkeeping bookkeeping) {
		Set<Asset> assetSet = new HashSet<>();
		for (AssetInitialData assetInitialData : AssetInitialData.values()) {
			for (String defaltAssetName : assetInitialData.getDefaltAssetNames()) {
				Asset asset = new Asset();
				asset.setBookkeeping(bookkeeping);
				asset.setAmount((long) 0);
				asset.setAssetType(assetInitialData.getAssetType());
				asset.setName(defaltAssetName);
				assetSet.add(asset);
			}
		}
		return assetRepository.save(assetSet);
	}
	
	public Asset save(Asset asset) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(asset.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != asset.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		return assetRepository.save(asset);
	}
	
	public Asset update(Asset asset) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(asset.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != asset.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		return assetRepository.save(asset);
	}

	@Transactional(value = "bookkeepingTransactionManager", readOnly = true)
	public Asset findOne(long id) {
		return assetRepository.findOne(id);
	}
	
	@Transactional(value = "bookkeepingTransactionManager", readOnly = true)
	public List<Asset> findByBookkeepingId(long bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}
	
	public void delete(Asset asset) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(asset.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != asset.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		assetRepository.delete(asset);
	}
	
	public void deleteBybookkeepingId(long bookkeepingId) {
		List<Asset> assetList = findByBookkeepingId(bookkeepingId);
		assetRepository.delete(assetList);
	}
}
