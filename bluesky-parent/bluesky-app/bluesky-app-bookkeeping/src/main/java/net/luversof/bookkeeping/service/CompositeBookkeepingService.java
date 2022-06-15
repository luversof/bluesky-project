package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;

@Service
public class CompositeBookkeepingService implements BookkeepingService {
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;
	
	@Autowired
	private BasicEntryGroupService entryGroupService;
	
	@Autowired
	private BasicAssetGroupService assetGroupService;
	
	@Autowired
	private BasicAssetService assetService;

	@Autowired
	private BasicEntryService entryService;

	/**
	 * 가계부 생성시 아래 default 데이터 생성
	 * 기본 자산 (asset)
	 * 기본 기록 그룹 (entryGroup)
	 * @param bookkeeping
	 * @return
	 */
	public Bookkeeping create(@BlueskyValidated(Bookkeeping.Create.class) Bookkeeping bookkeeping) {
		if (bookkeeping.getUserId() == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
		}

		bookkeepingService.create(bookkeeping);
		List<AssetGroup> assetGroupList = assetGroupService.createInitialData(bookkeeping.getBookkeepingId());
		assetService.createInitialData(bookkeeping.getBookkeepingId(), assetGroupList);
		entryGroupService.createInitialData(bookkeeping.getBookkeepingId());
		return bookkeeping;
	}
	
	public Optional<Bookkeeping> findByBookkeepingId(String bookkeepingId) {
		return bookkeepingService.findByBookkeepingId(bookkeepingId);
	}
	

	@Override
	public List<Bookkeeping> findByUserId(String userId) {
		return bookkeepingService.findByUserId(userId);
	}

	/**
	 * 유저의 가계부 변경
	 *
	 * @param bookkeeping
	 * @return
	 */
	public Bookkeeping update(@BlueskyValidated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}

	/**
	 * 완전 삭제의 경우 관련한 데이터를 모두 삭제 처리
	 * @param bookkeeping
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public void delete(@BlueskyValidated(Bookkeeping.Delete.class) Bookkeeping bookkeeping) {
		String bookkeepingId = bookkeeping.getBookkeepingId();
		bookkeepingService.delete(bookkeeping);
		entryService.deleteByBookkeepingId(bookkeepingId);
		entryGroupService.deleteAllBybookkeepingId(bookkeepingId);
		assetService.deleteAllByBookkeepingId(bookkeepingId);
		assetGroupService.deleteAllByBookkeepingId(bookkeepingId);
	}

}
