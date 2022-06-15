package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.bookkeeping.domain.EntryGroup;

public class CompositeEntryGroupService implements EntryGroupService {

	@Autowired
	private BasicEntryGroupService entryGroupService;
	
	@Override
	//@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<EntryGroup> createInitialData(String bookkeepingId) {
		return entryGroupService.createInitialData(bookkeepingId);
	}
	
	@Override
	public EntryGroup create(EntryGroup entryGroup) {
		return entryGroupService.create(entryGroup);
	}
	
	@Override
	public Optional<EntryGroup> findByEntryGroupId(String entryGroupId) {
		return entryGroupService.findByEntryGroupId(entryGroupId);
	}
	
	@Override
	public List<EntryGroup> findByBookkeepingId(String bookkeepingId) {
		return entryGroupService.findByBookkeepingId(bookkeepingId);
	}
	
	@Override
	public EntryGroup update(EntryGroup entryGroup) {
		return entryGroupService.update(entryGroup);
	}

	@Override
	public void delete(EntryGroup entryGroup) {
		entryGroupService.delete(entryGroup);
	}
	
	@Override
	public void deleteAllBybookkeepingId(String bookkeepingId) {
		entryGroupService.deleteAllBybookkeepingId(bookkeepingId);
	}
}
