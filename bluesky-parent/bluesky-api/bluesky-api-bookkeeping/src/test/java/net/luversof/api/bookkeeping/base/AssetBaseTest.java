package net.luversof.api.bookkeeping.base;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.service.base.AssetBaseService;

@Slf4j
public class AssetBaseTest implements GeneralTest {
	

	@Autowired
	private AssetBaseService assetBaseService;

	private UUID bookkeepingId = TestConstant.BOOKKEEPING_ID;

	
	@Test
	@DisplayName("bookkeepingId 의 asset 목록 조회")
	void findByBookkeepingId() {
		var assetList = assetBaseService.findByBookkeepingId(bookkeepingId);
		log.debug("assetList : {}", assetList);
	}
}
