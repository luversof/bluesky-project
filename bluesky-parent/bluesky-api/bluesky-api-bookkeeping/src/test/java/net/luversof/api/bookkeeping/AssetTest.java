package net.luversof.api.bookkeeping;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.constant.TestConstant;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.repository.mariadb.AssetTypeRepository;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.BookkeepingService;

@Slf4j
class AssetTest implements GeneralTest {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private AssetRepository assetRepository;
	
	@Autowired
	private AssetTypeRepository assetTypeRepository;

	@Autowired
	private AssetService assetService;

	private UUID userId = TestConstant.USER_ID;
	
	@BeforeEach
	void beforeEach() {
		
	}
	
	@Test
	void findAll() {
		List<Asset> assetList = assetRepository.findAll();
		log.debug("assetList : {}", assetList);
	}
	
	@Test
	void findById() {
		var assetOptional = assetService.findById(UUID.randomUUID());
		log.debug("assetOptional : {}", assetOptional);
	}
	
	
	@Test
	void save() {
		
		var bookkeeping = bookkeepingService.findByUserId(userId).get(0);
		
		var assetType = assetTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
		
		
		Asset account = new Asset();
		account.setName("테스트account");
		account.setBookkeeping(bookkeeping);
		account.setAssetTypeId(assetType.getId());
		var result = assetRepository.save(account);
		log.debug("result : {}", result);
	}
}
