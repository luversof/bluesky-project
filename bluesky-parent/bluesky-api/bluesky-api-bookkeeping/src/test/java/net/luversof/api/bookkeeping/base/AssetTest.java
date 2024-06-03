package net.luversof.api.bookkeeping.base;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.bookkeeping.base.domain.Asset;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetTypeRepository;
import net.luversof.api.bookkeeping.base.repository.mariadb.LedgerRepository;
import net.luversof.api.bookkeeping.base.service.AssetService;
import net.luversof.api.bookkeeping.constant.TestConstant;

@Slf4j
class AssetTest implements GeneralTest {
	
	@Autowired
	private LedgerRepository ledgerRepository;
	
	@Autowired
	private AssetRepository assetRepository;
	
	@Autowired
	private AssetTypeRepository assetTypeRepository;

	@Autowired
	private AssetService assetService;
	
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
		
		var ledger = ledgerRepository.findByUserId(TestConstant.USER_ID).get(0);
		
		var assetType = assetTypeRepository.findByLedgerId(ledger.getId()).get(0);
		
		
		Asset account = new Asset();
		account.setName("테스트account");
		account.setLedgerId(ledger.getId());
		account.setAssetTypeId(assetType.getId());
		var result = assetRepository.save(account);
		log.debug("result : {}", result);
	}
}
