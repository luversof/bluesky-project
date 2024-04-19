//package net.luversof.api.bookkeeping.service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.constant.BookkeepingConstants;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//import net.luversof.api.bookkeeping.domain.AssetGroup;
//
//@Service
//public class CompositeBookkeepingService implements BookkeepingService {
//	
//	@Autowired
//	private BasicBookkeepingService bookkeepingService;
//	
//	@Autowired
//	private BasicEntryGroupService entryGroupService;
//	
//	@Autowired
//	private BasicAssetGroupService assetGroupService;
//	
//	@Autowired
//	private BasicAssetService assetService;
//
//	@Autowired
//	private BasicEntryService entryService;
//
//	/**
//	 * 가계부 생성시 아래 default 데이터 생성
//	 * 기본 자산 (asset)
//	 * 기본 기록 그룹 (entryGroup)
//	 * @param bookkeeping
//	 * @return
//	 */
//	public Bookkeeping create(Bookkeeping bookkeeping) {
//		if (bookkeeping.getUserId() == null) {
//			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
//		}
//
//		bookkeepingService.create(bookkeeping);
//		List<AssetGroup> assetGroupList = assetGroupService.createInitialData(bookkeeping.getId());
//		assetService.createInitialData(bookkeeping.getId(), assetGroupList);
//		entryGroupService.createInitialData(bookkeeping.getId());
//		return bookkeeping;
//	}
//	
//	@Override
//	public Optional<Bookkeeping> findById(UUID bookkeepingId) {
//		return bookkeepingService.findById(bookkeepingId);
//	}
//
//	@Override
//	public List<Bookkeeping> findByUserId(UUID userId) {
//		return bookkeepingService.findByUserId(userId);
//	}
//
//	/**
//	 * 유저의 가계부 변경
//	 *
//	 * @param bookkeeping
//	 * @return
//	 */
//	public Bookkeeping update(Bookkeeping bookkeeping) {
//		return bookkeepingService.update(bookkeeping);
//	}
//
//	/**
//	 * 완전 삭제의 경우 관련한 데이터를 모두 삭제 처리
//	 * @param bookkeeping
//	 */
//	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
//	public void delete(Bookkeeping bookkeeping) {
//		var bookkeepingId = bookkeeping.getId();
//		bookkeepingService.delete(bookkeeping);
//		entryService.deleteByBookkeepingId(bookkeepingId);
//		entryGroupService.deleteAllBybookkeepingId(bookkeepingId);
//		assetService.deleteAllByBookkeepingId(bookkeepingId);
//		assetGroupService.deleteAllByBookkeepingId(bookkeepingId);
//	}
//
//}
