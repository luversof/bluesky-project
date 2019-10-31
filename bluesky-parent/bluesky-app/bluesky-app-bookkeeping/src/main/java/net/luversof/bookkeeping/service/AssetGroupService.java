package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.AssetGroupInitialData;
import net.luversof.bookkeeping.constant.BookkeepingConstants;
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
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<AssetGroup> initialDataSave(Bookkeeping bookkeeping) {
		return assetGroupRepository.saveAll(AssetGroupInitialData.getAssetGroupList(bookkeeping));
	}
	
	public List<AssetGroup> getUserAssetGroupList(UUID userId) {
		Bookkeeping userBookkeeping = bookkeepingService.getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return assetGroupRepository.findByBookkeepingId(userBookkeeping.getId());
	}
}
