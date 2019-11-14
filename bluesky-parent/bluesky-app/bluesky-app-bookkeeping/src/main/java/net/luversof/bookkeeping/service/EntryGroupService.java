package net.luversof.bookkeeping.service;


import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.constant.EntryGroupInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.repository.EntryGroupRepository;
import net.luversof.boot.exception.BlueskyException;

@Service
public class EntryGroupService {

	@Autowired
	private EntryGroupRepository entryGroupRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	//@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<EntryGroup> initialDataSave(Bookkeeping bookkeeping) {
		return entryGroupRepository.saveAll(EntryGroupInitialData.getEntryGroupList(bookkeeping));
	}
	
	public EntryGroup createUserEntryGroup(EntryGroup entryGroup) {
		entryGroup.setBookkeeping(bookkeepingService.getUserBookkeeping(entryGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING)));
		return entryGroupRepository.save(entryGroup);
	}
	
	public List<EntryGroup> getUserEntryGroupList(UUID userId) {
		Bookkeeping userBookkeeping = bookkeepingService.getUserBookkeeping(userId).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		return entryGroupRepository.findByBookkeepingId(userBookkeeping.getId());
	}
	
	public EntryGroup updateUserEntryGroup(EntryGroup entryGroup) {
		bookkeepingService.getUserBookkeeping(entryGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		EntryGroup targetEntryGroup = entryGroupRepository.findById(entryGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUP));
		if (targetEntryGroup.getBookkeeping().getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		return entryGroupRepository.save(entryGroup);
	}

	public void deleteUserEntryGroup(EntryGroup entryGroup) {
		bookkeepingService.getUserBookkeeping(entryGroup.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		EntryGroup targetEntryGroup = entryGroupRepository.findById(entryGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUP));
		if (targetEntryGroup.getBookkeeping().getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		entryGroupRepository.delete(entryGroup);
	}
	
	public void deleteBybookkeepingId(UUID bookkeepingId) {
		List<EntryGroup> entryGroupList = entryGroupRepository.findByBookkeepingId(bookkeepingId);
		entryGroupRepository.deleteAll(entryGroupList);
	}
}
