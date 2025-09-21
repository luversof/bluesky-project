package net.luversof.api.bookkeeping;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.repository.mariadb.AssetTypeRepository;
import net.luversof.api.bookkeeping.service.AssetTypeService;

@Slf4j
class AssetTypeTest implements GeneralTest {

	@Autowired
	AssetTypeService assetTypeService;
	
	@Autowired
	AssetTypeRepository assetTypeRepository;
	
	UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;
	
	@Test
	void createAssetType() {
		var assetType = new AssetType();
		assetType.setBookkeepingId(bookkeepingId);
		assetType.setCode(AssetTypeCode.INVESTMENT);
		assetType.setName("연금저축");
		var result = assetTypeService.createAssetType(assetType);
		log.debug("result : {}", result);
	}
	
	@Test
	void selectAssetType() {
		var assetTypeList = assetTypeRepository.findByBookkeepingId(bookkeepingId);
		assetTypeList.forEach(assetType -> log.debug("assetType : {}", assetType));
	}
}
