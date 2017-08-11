package net.luversof.bookkeeping.service;


import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.CoreErrorCode;
import net.luversof.user.service.LoginUserService;

@Service
public class BookkeepingService {

	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private LoginUserService loginUserService;
	
	/**
	 * 가계부 생성시 아래 default 데이터 생성
	 * 기본 자산 (asset)
	 * 기본 기록 그룹 (entryGroup)
	 * @param bookkeeping
	 * @return
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public Bookkeeping create(Bookkeeping bookkeeping) {
		bookkeeping.setUserId(loginUserService.getUserId().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID)));
		bookkeepingRepository.save(bookkeeping);
		assetService.initialDataSave(bookkeeping);
		entryGroupService.initialDataSave(bookkeeping);
		return bookkeeping;
	}
	
	public Bookkeeping update(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = findById(bookkeeping.getId());
		if (targetBookkeeping.getUserId() != bookkeeping.getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		
		return bookkeepingRepository.save(bookkeeping);
	}
	
	public Bookkeeping findById(UUID id) {
		return bookkeepingRepository.findById(id).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
	}
	
	public List<Bookkeeping> findByUserId(UUID userId) {
		return bookkeepingRepository.findByUserId(userId);
	}
	
	/**
	 * 삭제의 경우 관련한 데이터를 모두 삭제 처리를 해야할까?
	 * @param bookkeeping
	 */
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public void delete(Bookkeeping bookkeeping) {
		Bookkeeping targetBookkeeping = findById(bookkeeping.getId());
		if (targetBookkeeping.getUserId() != bookkeeping.getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		
		entryGroupService.deleteBybookkeepingId(bookkeeping.getId());
		assetService.deleteBybookkeepingId(bookkeeping.getId());
		bookkeepingRepository.delete(bookkeeping);
	}
}
