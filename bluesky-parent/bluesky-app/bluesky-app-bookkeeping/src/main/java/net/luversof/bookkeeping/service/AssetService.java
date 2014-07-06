package net.luversof.bookkeeping.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class AssetService {

	@Autowired
	private AssetRepository assetRepository;
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	public List<Asset> initialDataSave(Bookkeeping bookkeeping) {
		Set<Asset> assetSet = new HashSet<>();
		for (AssetInitialData assetInitialData : AssetInitialData.values()) {
			for (String defaltAssetName : assetInitialData.getDefaltAssetNames()) {
				Asset asset = new Asset();
				asset.setBookkeeping(bookkeeping);
				asset.setAmount((long) 0);
				asset.setAssetType(assetInitialData.getAssetType());
				asset.setName(defaltAssetName);
				assetSet.add(asset);
			}
		}
		return assetRepository.save(assetSet);
	}
	
	public Asset save(Asset asset) {
		return assetRepository.save(asset);
	}

	@Transactional(readOnly = true)
	public Asset findOne(long id) {
		return assetRepository.findOne(id);
	}
	
	@Transactional(readOnly = true)
	public List<Asset> findByBookkeepingId(long bookkeeping_id) {
		return assetRepository.findByBookkeepingId(bookkeeping_id);
	}
	
	public void delete(Asset asset) {
		assetRepository.delete(asset);
	}
	
	public void deleteBybookkeepingId(long bookkeeping_id) {
		List<Asset> assetList = findByBookkeepingId(bookkeeping_id);
		assetRepository.delete(assetList);
	}
}
