package net.luversof.asset.service;

import net.luversof.asset.domain.Asset;
import net.luversof.asset.repository.AssetRepository;
import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.ASSET)
public class AssetService {

	@Autowired
	private AssetRepository assetRepository;
	
	public Asset save(Asset asset) {
		return assetRepository.save(asset);
	}

	@Transactional(readOnly = true)
	public Asset findOne(long id) {
		return assetRepository.findOne(id);
	}
	
	public void delete(long id) {
		assetRepository.delete(id);
	}
}
