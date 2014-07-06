package net.luversof.bookkeeping.domain;

import lombok.Getter;

public enum AssetInitialData {

	WALLET(AssetType.WALLET, new String[]{ "지갑" });
	
	@Getter
	private AssetType assetType;
	
	@Getter
	private String[] defaltAssetNames;
	
	AssetInitialData(AssetType assetType, String[] defaltAssetNames) {
		this.assetType = assetType;
		this.defaltAssetNames = defaltAssetNames;
	}
}