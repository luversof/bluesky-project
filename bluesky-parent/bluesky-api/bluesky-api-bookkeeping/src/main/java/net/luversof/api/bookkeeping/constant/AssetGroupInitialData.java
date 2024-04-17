package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.MessageUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.domain.AssetGroup;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AssetGroupInitialData {
	WALLET("wallet", null),									//지갑 -> 현금으로 바꿀까 이름?
	BANK("bank", null),											//은행
	CREDITCARD("creditcard", AssetGroupType.CREDITCARD),		//신용카드
	CHECKCARD("checkcard", AssetGroupType.CHECKCARD),			//체크카드
	LOAN("loan", null)        									//대츨
	;
	
	private String messageCode;
	private AssetGroupType assetGroupType;
	
	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.asset-group.{0}.name", getMessageCode()));
	}
	
	public static List<AssetGroup> createAssetGroupList(UUID bookkeepingId) {
		List<AssetGroup> assetGroupList = new ArrayList<>();
		
		Arrays.asList(AssetGroupInitialData.values()).forEach(assetGroupInitialData -> {
			AssetGroup assetGroup = new AssetGroup();
			assetGroup.setBookkeepingId(bookkeepingId);
			assetGroup.setName(assetGroupInitialData.getName());
			assetGroup.setAssetGroupType(assetGroupInitialData.getAssetGroupType());
			assetGroupList.add(assetGroup);
		});
		
		return assetGroupList;
	}
	
	public AssetGroup createAssetGroup(UUID bookkeepingId) {
		var assetGroup = new AssetGroup();
		assetGroup.setBookkeepingId(bookkeepingId);
		assetGroup.setAssetGroupType(getAssetGroupType());
		assetGroup.setName(getName());
		return new AssetGroup();
	}
}
