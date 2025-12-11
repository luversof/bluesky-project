package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.domain.Bookkeeping;

/**
 * 기본 생성하여 제공하는 Account
 */
public enum AssetInitialData {

	CONTRA_ASSET(AssetTypeInitialData.CONTRA_ASSET.getCode(), AssetJsonConfigConstant.getImmutableConfigList()),
	WALLET(AssetTypeInitialData.CASH.getCode(), AssetJsonConfigConstant.getCustomConfigList());

	private static final Logger log = LoggerFactory.getLogger(AssetInitialData.class);

	private AssetTypeCode assetTypeCode;
	private Map<String, Object> jsonConfig;

	AssetInitialData(AssetTypeCode assetTypeCode, Map<String, Object> jsonConfig) {
		this.assetTypeCode = assetTypeCode;
		this.jsonConfig = jsonConfig;
	}

	public AssetTypeCode getAssetTypeCode() {
		return assetTypeCode;
	}

	public Map<String, Object> getJsonConfig() {
		return jsonConfig;
	}

	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account.{0}", name()), name());
	}

	public static List<Asset> getInitialData(Bookkeeping bookkeeping, List<AssetType> assetTypeList) {
		var assetList = new ArrayList<Asset>();

		for (var assetInitialData : AssetInitialData.values()) {
			var targetAssetType = assetTypeList.stream()
					.filter(accountType -> assetInitialData.assetTypeCode.equals(accountType.getCode())).findFirst()
					.orElseGet(() -> null);
			if (targetAssetType == null) {
				log.debug("targetAccoutType is not exist : {}", assetInitialData.assetTypeCode);
				continue;
			}

			var asset = new Asset();
			asset.setBookkeepingId(bookkeeping.getId());
			asset.setName(assetInitialData.getLocalizedName());
			asset.setAssetTypeId(targetAssetType.getId());
			asset.setJsonConfig(assetInitialData.getJsonConfig());
			assetList.add(asset);
		}

		return assetList;
	}

}
