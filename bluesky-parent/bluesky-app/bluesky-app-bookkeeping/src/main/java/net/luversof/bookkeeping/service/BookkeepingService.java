package net.luversof.bookkeeping.service;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;

@Service
public class BookkeepingService {

	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private AssetGroupService assetGroupService;
	
	@Autowired
	private AssetService assetService;

	@Autowired
	private EntryService entryService;

	/**
	 * 가계부 생성시 아래 default 데이터 생성
	 * 기본 자산 (asset)
	 * 기본 기록 그룹 (entryGroup)
	 * @param bookkeeping
	 * @return
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public Bookkeeping createUserBookkeeping(Bookkeeping bookkeeping) {
		if (bookkeeping.getUserId() == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
		}

		bookkeepingRepository.save(bookkeeping);
		List<AssetGroup> assetGroupList = assetGroupService.initialDataSave(bookkeeping);
		assetService.initialDataSave(bookkeeping, assetGroupList);
		entryGroupService.initialDataSave(bookkeeping);
		return bookkeeping;
	}

	/**
	 * userId 기반으로 bookkeeping 정보를 조회한다.
	 * N개 이상 있는 경우는 첫번째 반환 처리함
	 * 
	 * @param bookkeeping
	 * @return
	 */
	public Optional<Bookkeeping> getUserBookkeeping(UUID userId) {
		if (userId == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
		}
		List<Bookkeeping> bookkeepingList = bookkeepingRepository.findByUserId(userId);
		
		if (bookkeepingList.isEmpty()) {
			return Optional.empty();
		}
		if (bookkeepingList.size() == 1) {
			return Optional.of(bookkeepingList.get(0));
		}

		return Optional.of(bookkeepingList.get(0));
	}

	/**
	 * 유저의 가계부 변경
	 *
	 * @param bookkeeping
	 * @return
	 */
	public Bookkeeping updateUserBookkeeping(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = getUserBookkeeping(bookkeeping.getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		if (bookkeeping.getBaseDate() > 0) {
			targetBookkeeping.setBaseDate(bookkeeping.getBaseDate());
		}

		if (bookkeeping.getName() != null) {
			targetBookkeeping.setName(bookkeeping.getName());
		}

		return bookkeepingRepository.save(targetBookkeeping);
	}

	/**
	 * 완전 삭제의 경우 관련한 데이터를 모두 삭제 처리
	 * @param bookkeeping
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public void deleteUserBookkeeping(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = getUserBookkeeping(bookkeeping.getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entryService.deleteByBookkeepingId(targetBookkeeping.getId());
		entryGroupService.deleteBybookkeepingId(targetBookkeeping.getId());
		assetService.deleteBybookkeepingId(targetBookkeeping.getId());
		assetGroupService.deleteByBookkeepingId(targetBookkeeping.getId());
		bookkeepingRepository.delete(targetBookkeeping);
	}
}
