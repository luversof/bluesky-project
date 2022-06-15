package net.luversof.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.autoconfigure.context.MessageUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AssetInitialData {
	// 초기 추가 데이터의 경우 assetGroup 별 1개만 저장하는 것을 전제조건으로 함
	WALLET("wallet", AssetGroupInitialData.WALLET);

	private String messageCode;
	private AssetGroupInitialData assetGroupInitialData;

	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.asset.{0}.name", getMessageCode()));
	}

	public static List<Asset> createAssetList(String bookkeepingId, List<AssetGroup> assetGroupList) {
		List<Asset> assetList = new ArrayList<>();

		Arrays.asList(AssetInitialData.values()).forEach(assetInitialData -> {
			Asset asset = new Asset();
			asset.setAssetId(UUID.randomUUID().toString());
			asset.setBookkeepingId(bookkeepingId);
			asset.setName(assetInitialData.getName());
			
			assetGroupList.stream().forEach(assetGroup -> {
				if (assetGroup.getName().equals(assetInitialData.getAssetGroupInitialData().getName())) {
					asset.setAssetGroupId(assetGroup.getAssetGroupId());
				}
			});
			assetList.add(asset);
		});

		return assetList;
	}
}