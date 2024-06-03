package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.base.domain.Asset;
import net.luversof.api.bookkeeping.base.domain.AssetType;

/**
 * 기본 생성하여 제공하는 Account
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum AssetInitialData {

	WALLET(AssetTypeInitialData.CASH)
	;
	
	private AssetTypeInitialData  assetTypeInitialData;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account.{0}", name()), name());
	}
	
	public static List<Asset> getInitialData(UUID ledgerId, List<AssetType> assetTypeList) {
		var assetList = new ArrayList<Asset>();
		
		for (var assetInitialData : AssetInitialData.values()) {
			var targetAssetType = assetTypeList.stream().filter(accountType -> assetInitialData.getAssetTypeInitialData().getCode().equals(accountType.getCode())).findFirst().orElseGet(() -> null);
			if (targetAssetType == null) {
				log.debug("targetAccoutType is not exist : {}", assetInitialData.getAssetTypeInitialData());
				continue;
			}
			
			var asset = new Asset();
			asset.setLedgerId(ledgerId);
			asset.setName(assetInitialData.getLocalizedName());
			asset.setAssetTypeId(targetAssetType.getId());
			assetList.add(asset);
		}
		
		return assetList;
	}
}
