package net.luversof.bookkeeping.service;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.constant.BookkeepingConstants;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.constant.EntryGroupInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.repository.EntryGroupRepository;
import net.luversof.core.exception.BlueskyException;

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
	@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<EntryGroup> initialDataSave(Bookkeeping bookkeeping) {
		Set<EntryGroup> entryGroupSet = new HashSet<>();
		for (EntryGroupInitialData entryGroupInitialData : EntryGroupInitialData.values()) {
			for (String defaultEntryGroupName : entryGroupInitialData.getDefaltEntryGroupNames()){
				EntryGroup entryGroup = new EntryGroup();
				entryGroup.setEntryType(entryGroupInitialData.getEntryType());
				entryGroup.setName(defaultEntryGroupName);
				entryGroup.setBookkeeping(bookkeeping);
				entryGroupSet.add(entryGroup);
			}
		}
		return entryGroupRepository.saveAll(entryGroupSet);
	}
	
	public EntryGroup create(EntryGroup entryGroup) {
		Bookkeeping targetBookkeeping = bookkeepingService.findById(entryGroup.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		return entryGroupRepository.save(entryGroup);
	}
	
	public EntryGroup update(EntryGroup entryGroup) {
		Bookkeeping targetBookkeeping = bookkeepingService.findById(entryGroup.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		EntryGroup targetEntryGroup = findOne(entryGroup.getId());
		if (targetEntryGroup.getBookkeeping().getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		return entryGroupRepository.save(entryGroup);
	}

	public EntryGroup findOne(long id) {
		return entryGroupRepository.getOne(id);
	}
	
	public List<EntryGroup> findByBookkeepingId(UUID bookkeepingId) {
		return entryGroupRepository.findByBookkeepingId(bookkeepingId);
	}

	public void delete(EntryGroup entryGroup) {
		Bookkeeping targetBookkeeping = bookkeepingService.findById(entryGroup.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_BOOKKEEPING);
		}
		EntryGroup targetEntryGroup = findOne(entryGroup.getId());
		if (targetEntryGroup.getBookkeeping().getUserId() != entryGroup.getBookkeeping().getUserId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		entryGroupRepository.delete(entryGroup);
	}
	
	public void deleteBybookkeepingId(UUID bookkeepingId) {
		List<EntryGroup> entryGroupList = findByBookkeepingId(bookkeepingId);
		entryGroupRepository.deleteAll(entryGroupList);
	}
}
