package net.luversof.api.bookkeeping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.service.base.AssetTypeBaseService;

@Service
public class AssetTypeService {

	@Setter(onMethod_ = @Autowired)
	private AssetTypeBaseService assetTypeBaseService;
	
	public AssetType createAssetType(AssetType assetType) {
		return assetTypeBaseService.save(assetType);
	}

}
