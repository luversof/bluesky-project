package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.bookkeeping.constant.BookkeepingError;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.repository.mariadb.AssetTypeRepository;
import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;
import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;

@Service
public class AssetService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingRepository bookkeepingRepository;
	
	@Setter(onMethod_ = @Autowired)
	private AssetRepository assetRepository;
	
	@Setter(onMethod_ = @Autowired)
	private EntryRepository entryRepository;
	
	@Setter(onMethod_ = @Autowired)
	private AssetTypeRepository assetTypeRepository;
	
	
	public Asset createAsset(Asset asset) {
		
		checkAsset(asset);
		
		// bitConfig set
//		if (asset.getBitConfigIndexList() == null) {
//			asset.setBitConfigIndexList(AssetInitialData.getNormalBitConfigList());
//		}

		return assetRepository.save(asset);
	}
	
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}
	
	public Asset updateAsset(Asset asset) {
		
		var targetAsset = assetRepository.findById(asset.getId()).orElseThrow(BookkeepingError.NOT_EXIST_ASSET::exception);
		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
			BookkeepingError.INVALID_BOOKKEEPING_ID.throwException();
		}
		
//		// bitConfig 체크를 해야 하는 경우는 어떻게 처리를 해야 할까?
//		var bitConfigIndexList = targetAsset.getBitConfigIndexList();
//		
//		
//		if (!AssetBitConfig.ENABLE_UPDATE.hasIndexFromIndexList(bitConfigIndexList)) {
//			BookkeepingError.INVALID_REQUEST.throwException();
//		}
		
		checkAsset(asset);
		
		return assetRepository.save(asset);
	}
	
	/**
	 * asset id를 기준으로 삭제 처리
	 * bookkeeping.id, bookkeeping.userId를 전달받아 체크해야 할듯?
	 * @param asset
	 */
	public void deleteAsset(Asset asset) {
		// 삭제 전 데이터가 올바른지 확인
		var targetAsset = assetRepository.findById(asset.getId()).orElseThrow(BookkeepingError.NOT_EXIST_ASSET::exception);
		// TODO bookkeeping.id, bookkeeping.userId를 전달받아 올바른지 체크
		
		// 해당 asset을 사용한 entry가 있는지 확인
		boolean isEnableDelete = entryRepository.findByIncomeAssetId(asset.getId()).isEmpty() && entryRepository.findByOutgoingAssetId(asset.getId()).isEmpty();
		
		// asset을 비노출 처리 하려면 entry에 대한 처리를 먼저 결정해야함.
		// 일단 삭제 불가 에러 처리를 하려고 함
		if (!isEnableDelete) {
			BookkeepingError.UNABLE_DELETE_ASSET.throwException();
		}
		assetRepository.delete(targetAsset);
	}

	/**
	 * Create와 Update에서 공통으로 확인하는 항목
	 * - 해당 asset의 bookkeepingId가 올바른가
	 * - 해당 asset의 assetType isNotNull
	 * - 해당 asset의 assetType이 해당 bookkeeping의 assetType인가.
	 * @param asset
	 */
	private void checkAsset(Asset asset) {
		var targetBookkeeping = bookkeepingRepository.findById(asset.getBookkeepingId()).orElseThrow(BookkeepingError.NOT_EXIST_BOOKKEEPING_ID::exception);
		
		if (asset.getAssetTypeId() == null || asset.getAssetTypeId() == null) {
			BookkeepingError.NOT_EXIST_ASSETTYPE_ID.throwException();
		}
		
		var targetAssetType = assetTypeRepository.findById(asset.getAssetTypeId()).orElseThrow(BookkeepingError.INVALID_ASSETTYPE_ID::exception);
		if (!targetAssetType.getBookkeepingId().equals(targetBookkeeping.getId())) {
			BookkeepingError.INVALID_ASSETTYPE_ID.throwException();
		}
		
	}

}
