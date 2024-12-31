package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.domain.Bookkeeping;

/**
 * 기본 생성하여 제공하는 Account
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum AssetInitialData {

	CONTRA_ASSET(AssetTypeInitialData.CONTRA_ASSET, getContraBitSet()),
	WALLET(AssetTypeInitialData.CASH, getNormalBitSet())
	;
	
	private AssetTypeInitialData assetTypeInitialData;
	private BitSet bitConfig;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account.{0}", name()), name());
	}
	
	public static List<Asset> getInitialData(Bookkeeping bookkeeping, List<AssetType> assetTypeList) {
		var assetList = new ArrayList<Asset>();
		
		for (var assetInitialData : AssetInitialData.values()) {
			var targetAssetType = assetTypeList.stream().filter(accountType -> assetInitialData.getAssetTypeInitialData().getCode().equals(accountType.getCode())).findFirst().orElseGet(() -> null);
			if (targetAssetType == null) {
				log.debug("targetAccoutType is not exist : {}", assetInitialData.getAssetTypeInitialData());
				continue;
			}
			
			var asset = new Asset();
			asset.setBookkeeping(bookkeeping);
			asset.setName(assetInitialData.getLocalizedName());
			asset.setAssetType(targetAssetType);
			asset.setBitConfig(assetInitialData.getBitConfig());
			assetList.add(asset);
		}
		
		return assetList;
	}
	
	private static BitSet getContraBitSet() {
		return new BitSet();
	}
	
	public static BitSet getNormalBitSet() {
		var bitSet =  new BitSet();
		bitSet.set(AssetBitConfig.ENABLE_DISPLAY.getBitIndex());
		return bitSet;
	}
}
