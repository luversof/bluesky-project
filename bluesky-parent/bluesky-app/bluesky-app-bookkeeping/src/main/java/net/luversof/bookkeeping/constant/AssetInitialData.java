package net.luversof.bookkeeping.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.boot.autoconfigure.context.MessageUtil;

import java.text.MessageFormat;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AssetInitialData {

	WALLET(AssetType.WALLET, "defaultWallet");
	
	@Getter
	private AssetType assetType;
	
	@Getter
	private String messageCode;
	
	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.asset.{0}.name", getMessageCode()));
	}

}