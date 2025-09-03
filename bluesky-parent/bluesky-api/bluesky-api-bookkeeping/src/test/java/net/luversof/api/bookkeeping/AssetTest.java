package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.AssetBitConfig;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.base.AssetTypeBaseService;
import net.luversof.api.bookkeeping.service.base.BookkeepingBaseService;

@Slf4j
class AssetTest implements GeneralTest {
	
	@Autowired
	private BookkeepingBaseService bookkeepingService;
	
	@Autowired
	private AssetTypeBaseService assetTypeBaseService;

	@Autowired
	private AssetService assetService;

	private UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;
	
	@BeforeEach
	void beforeEach() {
		
	}
	
	@Test
	void createAsset() {
		
		var bookkeeping = bookkeepingService.findById(bookkeepingId).orElseThrow();
		var assetType = assetTypeBaseService.findByBookkeepingId(bookkeepingId).get(0);
		
		var asset = new Asset();
		asset.setBookkeepingId(AggregateReference.to(bookkeeping.getId()));
		asset.setAssetType(assetType);
		asset.setName("테스트자산");
		
		
		var result = assetService.createAsset(asset);
		log.debug("result : {}", result);
		assertThat(result).isNotNull();
	}
	
	@Test
	void updateAsset() {
		var assetList = assetService.findByBookkeepingId(bookkeepingId);
		var targetAsset = assetList.stream()
//				.filter(asset -> asset.getBitConfigIndexList().contains(AssetBitConfig.ENABLE_UPDATE.getIndex()))
				.findAny().get();
		
		targetAsset.setName(targetAsset.getName() + " 수정");
		
		var result = assetService.updateAsset(targetAsset);
		assertThat(result).isNotNull();
	}

}
