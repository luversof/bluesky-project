package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.bookkeeping.constant.AssetInitialData;
import net.luversof.api.bookkeeping.constant.ErrorCode;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.base.AssetBaseService;
import net.luversof.api.bookkeeping.service.base.AssetTypeBaseService;
import net.luversof.api.bookkeeping.service.base.BookkeepingBaseService;

@Service
public class AssetService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingBaseService bookkeepingBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetBaseService assetBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetTypeBaseService assetTypeBaseService;
	
	
	public Asset createAsset(Asset asset) {
		checkAsset(asset);
		
		// bitConfig set
		if (asset.getBitConfigIndexList() == null) {
			asset.setBitConfigIndexList(AssetInitialData.getNormalBitConfigIndexList());
		}

		return assetBaseService.save(asset);
	}
	
	public Asset updateAsset(Asset asset) {
		
		var targetAsset = assetBaseService.findById(asset.getId()).orElseThrow(ErrorCode.NOT_EXIST_ASSET::exception);
		if (!targetAsset.getBookkeeping().getId().equals(asset.getBookkeeping().getId())) {
			ErrorCode.INVALID_BOOKKEEPING_ID.throwException();
		}
		
		// bitConfig 체크를 해야 하는 경우는 어떻게 처리를 해야 할까?
		
		checkAsset(asset);
		
		return assetBaseService.update(asset);
	}
	
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetBaseService.findByBookkeepingId(bookkeepingId);
	}
	
	public void deleteAsset(Asset asset) {
		// 해당 asset에 대한 entry가 있는 경우 삭제를 하면 안되는데 어떻게 처리할까? 
	}

	
	private void checkAsset(Asset asset) {
		if (bookkeepingBaseService.findById(asset.getBookkeeping().getId()).isEmpty()) {
			ErrorCode.NOT_EXIST_BOOKKEEPING_ID.throwException();
		}
		
		if (asset.getAssetType() == null || asset.getAssetType().getId() == null) {
			ErrorCode.NOT_EXIST_ASSETTYPE_ID.throwException();
		}
		
		if (assetTypeBaseService.findById(asset.getAssetType().getId()).isEmpty()) {
			ErrorCode.INVALID_ASSETTYPE_ID.throwException();
		}
	}

}
