package net.luversof.asset;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.asset.domain.Asset;
import net.luversof.asset.domain.AssetGroup;
import net.luversof.asset.service.AssetGroupService;
import net.luversof.asset.service.AssetService;
import net.luversof.core.config.GeneralTest;

@Slf4j
public class AssetTest extends GeneralTest {
	
	@Autowired
	private AssetService assetService;

	@Autowired
	private AssetGroupService assetGroupService;
	
	@Test
	@Ignore
	public void assetGroup추가() {
		
		AssetGroup assetGroup = new AssetGroup();
		assetGroup.setAssetType_id(1);
		assetGroup.setName("test");
		assetGroup.setUsername("bluesky");
		AssetGroup resultAssetGroup = assetGroupService.save(assetGroup);
		log.debug("resultAssetGroup : {}", resultAssetGroup);
	}
	@Test
	@Ignore
	public void test() {
		
		AssetGroup assetGroup = assetGroupService.findOne(1);
		
		for (int i = 1 ; i < 100; i++) {
			
			Asset asset = new Asset();
			asset.setName("test" + i);
			asset.setUsername("bluesky");
			asset.setAmount(100);
			asset.setAssetGroup(assetGroup);
			asset.setEnable(true);
			Asset resultAsset = assetService.save(asset);
			log.debug("result : {}", resultAsset);
		}
	}
	
	@Test
	public void test2() {
		List<Asset> assetList = assetService.findByUsername("bluesky");
		log.debug("assetList : {}", assetList);
	}
}
