package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.AssetGroupInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetGroupRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class AssetGroupService {
	
	@Autowired
	private AssetGroupRepository assetGroupRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<AssetGroup> initialDataSave(Bookkeeping bookkeeping) {
		return assetGroupRepository.saveAll(AssetGroupInitialData.getAssetGroupList(bookkeeping));
	}
	
	public AssetGroup createUserAssetGroup(AssetGroup assetGroup) {
		assetGroup.setBookkeeping(bookkeepingService.getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING)));
		return assetGroupRepository.save(assetGroup);
	}
	
	public List<AssetGroup> getUserAssetGroupList(UUID userId) {
		Bookkeeping userBookkeeping = bookkeepingService.getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetGroupRepository.findByBookkeepingId(userBookkeeping.getId());
	}
	
	/**
	 * 이름만 바꿀수 있음
	 * @param assetGroup
	 * @return
	 */
	public AssetGroup updateUserAssetGroup(AssetGroup assetGroup) {
		Bookkeeping bookkeeping = bookkeepingService.getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		AssetGroup targetAssetGroup = assetGroupRepository.findById(assetGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
		if (!targetAssetGroup.getBookkeeping().getUserId().equals(bookkeeping.getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSETGROUP);
		}
		targetAssetGroup.setName(assetGroup.getName());
		
		return assetGroupRepository.save(targetAssetGroup);
	}
	
	public void deleteUserAssetGroup(AssetGroup assetGroup) {
		bookkeepingService.getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		AssetGroup targetAssetGroup = assetGroupRepository.findById(assetGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
		if (!targetAssetGroup.getBookkeeping().getUserId().equals(assetGroup.getBookkeeping().getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSETGROUP);
		}
		assetGroupRepository.delete(targetAssetGroup);
	}
	
	public void deleteByBookkeepingId(UUID bookkeepingId) {
		List<AssetGroup> assetGroupList = assetGroupRepository.findByBookkeepingId(bookkeepingId);
		assetGroupRepository.deleteAll(assetGroupList);
	}
}
