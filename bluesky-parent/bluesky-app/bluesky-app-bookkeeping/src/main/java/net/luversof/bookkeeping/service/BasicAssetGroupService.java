package net.luversof.bookkeeping.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.AssetGroupInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.repository.AssetGroupRepository;

@Service
public class BasicAssetGroupService implements AssetGroupService {
	
	@Autowired
	private AssetGroupRepository assetGroupRepository;
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<AssetGroup> createInitialData(String bookkeepingId) {
		return assetGroupRepository.saveAll(AssetGroupInitialData.createAssetGroupList(bookkeepingId));
	}
	
	@Override
	public AssetGroup create(AssetGroup assetGroup) {
		// bookkeepingId가 존재하는 것인지 체크를 하려면?
		return assetGroupRepository.save(assetGroup);
	}
	
	@Override
	public List<AssetGroup> findByBookkeepingId(String bookkeepingId) {
		return assetGroupRepository.findByBookkeepingId(bookkeepingId);
	}
	
	@Override
	public AssetGroup update(AssetGroup assetGroup) {
		AssetGroup targetAssetGroup = assetGroupRepository.findByAssetGroupId(assetGroup.getAssetGroupId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
		targetAssetGroup.setName(assetGroup.getName());
		return assetGroupRepository.save(targetAssetGroup);
	}
	
	@Override
	public void delete(AssetGroup assetGroup) {
		AssetGroup targetAssetGroup = assetGroupRepository.findByAssetGroupId(assetGroup.getAssetGroupId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
		assetGroupRepository.delete(targetAssetGroup);
	}
	
	@Override
	public void deleteAllByBookkeepingId(String bookkeepingId) {
		List<AssetGroup> assetGroupList = assetGroupRepository.findByBookkeepingId(bookkeepingId);
		assetGroupRepository.deleteAll(assetGroupList);
	}
}
