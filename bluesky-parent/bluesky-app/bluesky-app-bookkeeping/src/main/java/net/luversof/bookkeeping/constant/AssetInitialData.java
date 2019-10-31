package net.luversof.bookkeeping.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.boot.autoconfigure.context.MessageUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AssetInitialData {

	WALLET("wallet", AssetGroupInitialData.WALLET);

	private String messageCode;
	private AssetGroupInitialData assetGroupInitialData;

	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.asset.{0}.name", getMessageCode()));
	}

	public static List<Asset> getAssetList(Bookkeeping bookkeeping, List<AssetGroup> assetGroupList) {
		List<Asset> assetList = new ArrayList<>();

		Arrays.asList(AssetInitialData.values()).forEach(assetInitialData -> {
			Asset asset = new Asset();
			asset.setName(assetInitialData.getName());
			asset.setBookkeeping(bookkeeping);
			asset.setAssetGroup(assetGroupList.stream().filter(
					assetGroup -> assetGroup.getName().equals(assetInitialData.getAssetGroupInitialData().getName()))
					.findFirst().orElse(null));
			assetList.add(asset);
		});

		return assetList;
	}
}