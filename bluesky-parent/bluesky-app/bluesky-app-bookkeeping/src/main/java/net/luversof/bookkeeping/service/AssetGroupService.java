package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.AssetGroupInitialData;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.AssetGroupRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;

@Service
public class AssetGroupService {
	
	@Autowired
	private AssetGroupRepository assetGroupRepository;
	
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
		assetGroup.setBookkeeping(BookkeepingUtils.getBookkeepingService().getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING)));
		return assetGroupRepository.save(assetGroup);
	}
	
	public List<AssetGroup> getUserAssetGroupList(UUID userId) {
		Bookkeeping userBookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetGroupRepository.findByBookkeepingId(userBookkeeping.getId());
	}
	
	/**
	 * 이름만 바꿀수 있음
	 * @param assetGroup
	 * @return
	 */
	public AssetGroup updateUserAssetGroup(AssetGroup assetGroup) {
		Bookkeeping bookkeeping = BookkeepingUtils.getBookkeepingService().getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		AssetGroup targetAssetGroup = assetGroupRepository.findById(assetGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
		if (!targetAssetGroup.getBookkeeping().getUserId().equals(bookkeeping.getUserId())) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ASSETGROUP);
		}
		targetAssetGroup.setName(assetGroup.getName());
		
		return assetGroupRepository.save(targetAssetGroup);
	}
	
	public void deleteUserAssetGroup(AssetGroup assetGroup) {
		BookkeepingUtils.getBookkeepingService().getUserBookkeeping(assetGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
