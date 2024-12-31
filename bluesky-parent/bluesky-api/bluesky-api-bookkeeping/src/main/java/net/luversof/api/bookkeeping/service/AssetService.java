package net.luversof.api.bookkeeping.service;

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
		if (bookkeepingBaseService.findById(asset.getBookkeeping().getId()).isEmpty()) {
			ErrorCode.NOT_EXIST_BOOKKEEPING_ID.throwException();
		}
		
		// bitConfig set
		if (asset.getBitConfig() == null) {
			asset.setBitConfig(AssetInitialData.getNormalBitSet());
		}
		
		if (asset.getAssetType() == null || asset.getAssetType().getId() == null) {
			ErrorCode.NOT_EXIST_ASSETTYPE_ID.throwException();
		}
		
		if (assetTypeBaseService.findById(asset.getAssetType().getId()).isEmpty()) {
			ErrorCode.INVALID_ASSETTYPE_ID.throwException();
		}
		
		return assetBaseService.save(asset);
	}

}
