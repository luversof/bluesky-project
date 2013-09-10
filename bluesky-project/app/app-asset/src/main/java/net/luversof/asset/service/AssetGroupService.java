package net.luversof.asset.service;

import java.util.List;

import net.luversof.asset.domain.AssetGroup;
import net.luversof.asset.repository.AssetGroupRepository;
import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.ASSET)
public class AssetGroupService {

	@Autowired
	private AssetGroupRepository assetGroupRepository;

	public AssetGroup save(AssetGroup assetGroup) {
		return assetGroupRepository.save(assetGroup);
	}

	@Transactional(readOnly = true)
	public AssetGroup findOne(long id) {
		return assetGroupRepository.findOne(id);
	}
	
	public void delete(long id) {
		assetGroupRepository.delete(id);
	}
	
	@Transactional(readOnly = true)
	public List<AssetGroup> findByUsername(String username) {
		return assetGroupRepository.findByUsername(username);
	}
}
