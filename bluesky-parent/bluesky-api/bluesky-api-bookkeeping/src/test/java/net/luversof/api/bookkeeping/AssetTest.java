package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.AssetJsonConfigConstant;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.repository.AssetTypeRepository;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.BookkeepingService;

@Slf4j
class AssetTest implements GeneralTest {
	
	@Autowired
	BookkeepingService bookkeepingService;
	
	@Autowired
	AssetTypeRepository assetTypeRepository;

	@Autowired
	AssetService assetService;

	UUID userId = TestConstant.USER_ID;
	
	@BeforeEach
	void beforeEach() {
		
	}
	
	private Bookkeeping getBookkeeping() {
		return bookkeepingService.findByUserId(userId).get(0);
	}
	
	@Test
	@DisplayName("자산 생성")
	void createAsset() {
		
		var bookkeeping = getBookkeeping();
		var assetType = assetTypeRepository.findByBookkeepingIdAndCode(bookkeeping.getId(), AssetTypeCode.CASH).get(0);
		
		var asset = new Asset();
		asset.setBookkeepingId(bookkeeping.getId());
		asset.setAssetTypeId(assetType.getId());
		asset.setName("테스트자산");
		
		
		var result = assetService.createAsset(asset);
		log.debug("result : {}", result);
		assertThat(result).isNotNull();
	}
	
	@Test
	void findByBookkeepingId() {
		
		var bookkeeping = getBookkeeping();
		var assetList = assetService.findByBookkeepingId(bookkeeping.getId());
		
		log.debug("assetList : {}", assetList);
		assertThat(assetList).isNotEmpty();
	}
	
	@Test
	void updateAsset() {
		
		var bookkeeping = getBookkeeping();
		var assetList = assetService.findByBookkeepingId(bookkeeping.getId());
		var targetAsset = assetList.stream()
				.filter(asset -> Boolean.TRUE.equals(asset.getJsonConfig().get(AssetJsonConfigConstant.ENABLE_UPDATE)))
				.findAny().get();
		
		targetAsset.setName(targetAsset.getName() + " 수정");
		
		var result = assetService.updateAsset(targetAsset);
		assertThat(result).isNotNull();
	}

}
