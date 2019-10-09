package net.luversof.bookkeeping.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class BookkeepingService {

	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
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
	public Bookkeeping create(Bookkeeping bookkeeping) {
		if (bookkeeping.getUserId() == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
		}

		bookkeepingRepository.save(bookkeeping);
		assetService.initialDataSave(bookkeeping);
		entryGroupService.initialDataSave(bookkeeping);
		return bookkeeping;
	}

	/**
	 * userId 기반으로 bookkeeping 정보를 조회한다.
	 * N개 이상 있는 경우 요청 시 bookkeepingId 필요
	 * @param bookkeeping
	 * @return
	 */
	public Optional<Bookkeeping> getUserBookkeeping(Bookkeeping bookkeeping) {
		if (bookkeeping.getUserId() == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
		}
		List<Bookkeeping> bookkeepingList = bookkeepingRepository.findByUserId(bookkeeping.getUserId());
		if (bookkeepingList.isEmpty()) {
			return Optional.empty();
		}
		if (bookkeepingList.size() == 1) {
			return Optional.of(bookkeepingList.get(0));
		}

		if (bookkeeping.getId() == null) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING_ID);
		}
		return bookkeepingList.stream().filter(x -> x.getId().equals(bookkeeping.getId())).findAny();
	}

	/**
	 * 유저의 가계부 변경
	 *
	 * @param bookkeeping
	 * @return
	 */
	public Bookkeeping update(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = getUserBookkeeping(bookkeeping).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
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
	public void delete(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = getUserBookkeeping(bookkeeping).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entryService.deleteByBookkeepingId(targetBookkeeping.getId());
		entryGroupService.deleteBybookkeepingId(targetBookkeeping.getId());
		assetService.deleteBybookkeepingId(targetBookkeeping.getId());
		bookkeepingRepository.delete(targetBookkeeping);
	}
}
