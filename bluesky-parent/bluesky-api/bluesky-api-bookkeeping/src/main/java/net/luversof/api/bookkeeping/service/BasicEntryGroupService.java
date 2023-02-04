package net.luversof.api.bookkeeping.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryGroupInitialData;
import net.luversof.api.bookkeeping.domain.EntryGroup;
import net.luversof.api.bookkeeping.repository.EntryGroupRepository;

@Service
public class BasicEntryGroupService implements EntryGroupService {

	@Autowired
	private EntryGroupRepository entryGroupRepository;
	
	@Override
	//@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<EntryGroup> createInitialData(String bookkeepingId) {
		return entryGroupRepository.saveAll(EntryGroupInitialData.createEntryGroupList(bookkeepingId));
	}
	
	@Override
	public EntryGroup create(EntryGroup entryGroup) {
		return entryGroupRepository.save(entryGroup);
	}
	
	@Override
	public Optional<EntryGroup> findByEntryGroupId(String entryGroupId) {
		return entryGroupRepository.findByEntryGroupId(entryGroupId);
	}
	
	@Override
	public List<EntryGroup> findByBookkeepingId(String bookkeepingId) {
		return entryGroupRepository.findByBookkeepingId(bookkeepingId);
		
	}
	
	@Override
	public EntryGroup update(EntryGroup entryGroup) {
		EntryGroup targetEntryGroup = findByEntryGroupId(entryGroup.getEntryGroupId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUP));
		if (targetEntryGroup.getBookkeepingId() != entryGroup.getBookkeepingId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		return entryGroupRepository.save(entryGroup);
	}

	@Override
	public void delete(EntryGroup entryGroup) {
		EntryGroup targetEntryGroup = findByEntryGroupId(entryGroup.getEntryGroupId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ENTRYGROUP));
		if (targetEntryGroup.getBookkeepingId() != entryGroup.getBookkeepingId()) {
			throw new BlueskyException(BookkeepingErrorCode.NOT_OWNER_ENTRYGROUP);
		}
		entryGroupRepository.delete(entryGroup);
	}
	
	@Override
	public void deleteAllBybookkeepingId(String bookkeepingId) {
		List<EntryGroup> entryGroupList = entryGroupRepository.findByBookkeepingId(bookkeepingId);
		entryGroupRepository.deleteAll(entryGroupList);
	}
}
