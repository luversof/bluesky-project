package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.repository.AssetRepository;
import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
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
	
	@Transactional(readOnly = true)
	public List<Asset> findByUsername(String username) {
		return assetRepository.findByUsername(username);
	}
}
