package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.domain.AssetGroup;

@Service
public class CompositeAssetGroupService implements AssetGroupService {
	
	@Autowired
	private BasicAssetGroupService assetGroupService;
	
	@Override
	public List<AssetGroup> createInitialData(UUID bookkeepingId) {
		return assetGroupService.createInitialData(bookkeepingId);
	}

	@Override
	public AssetGroup create(AssetGroup assetGroup) {
		return assetGroupService.create(assetGroup);
	}

	@Override
	public List<AssetGroup> findByBookkeepingId(UUID bookkeepingId) {
		return assetGroupService.findByBookkeepingId(bookkeepingId);
	}

	@Override
	public AssetGroup update(AssetGroup assetGroup) {
		return assetGroupService.update(assetGroup);
	}

	@Override
	public void delete(AssetGroup assetGroup) {
		assetGroupService.delete(assetGroup);		
	}

	@Override
	public void deleteAllByBookkeepingId(UUID bookkeepingId) {
		assetGroupService.deleteAllByBookkeepingId(bookkeepingId);
	}

}
