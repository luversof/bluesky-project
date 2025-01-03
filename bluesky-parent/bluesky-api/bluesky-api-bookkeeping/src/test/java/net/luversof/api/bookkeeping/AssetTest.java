package net.luversof.api.bookkeeping;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
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
		asset.setBookkeeping(bookkeeping);
		asset.setAssetType(assetType);
		asset.setName("테스트자산");
		
		
		var result = assetService.createAsset(asset);
		log.debug("result : {}", result);
	}
	
	@Test
	void updateAsset() {
		var assetList = assetService.findByBookkeepingId(bookkeepingId);
		assetList.get(0);
		
		
		
	}
}
