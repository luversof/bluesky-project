package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.jdbc.core.mapping.AggregateReference;

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

	CONTRA_ASSET(AssetTypeInitialData.CONTRA_ASSET, getContraBitConfigList()),
	WALLET(AssetTypeInitialData.CASH, getNormalBitConfigList())
	;
	
	private AssetTypeInitialData assetTypeInitialData;
	private List<Integer> bitConfigIndexList;
	
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
			asset.setBookkeepingId(AggregateReference.to(bookkeeping.getId()));
//			asset.setBookkeeping(bookkeeping);
			asset.setName(assetInitialData.getLocalizedName());
			asset.setAssetType(targetAssetType);
//			asset.setBitConfigIndexList(assetInitialData.getBitConfigIndexList());
			assetList.add(asset);
		}
		
		return assetList;
	}
	
	private static List<Integer> getContraBitConfigList() {
		return Collections.emptyList();
	}
	
	public static List<Integer> getNormalBitConfigList() {
		var list = new ArrayList<Integer>();
		list.add(AssetBitConfig.ENABLE_DELETE.getIndex());
		list.add(AssetBitConfig.ENABLE_UPDATE.getIndex());
		list.add(AssetBitConfig.ENABLE_DISPLAY.getIndex());
		return list;
	}
}
