package net.luversof.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.boot.autoconfigure.context.MessageUtil;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AssetGroupInitialData {
	WALLET("wallet", null),										//지갑
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
	
	public static List<AssetGroup> getAssetGroupList(Bookkeeping bookkeeping) {
		List<AssetGroup> assetGroupList = new ArrayList<>();
		
		Arrays.asList(AssetGroupInitialData.values()).forEach(assetGroupInitialData -> {
			AssetGroup assetGroup = new AssetGroup();
			assetGroup.setName(assetGroupInitialData.getName());
			assetGroup.setBookkeeping(bookkeeping);
			assetGroup.setAssetGroupType(assetGroupInitialData.getAssetGroupType());
			assetGroupList.add(assetGroup);
		});
		
		return assetGroupList;
	}
}
