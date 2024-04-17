package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.domain.EntryGroup;

@Service
public class CompositeEntryGroupService implements EntryGroupService {

	@Autowired
	private BasicEntryGroupService entryGroupService;
	
	@Override
	//@Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
	public List<EntryGroup> createInitialData(UUID bookkeepingId) {
		return entryGroupService.createInitialData(bookkeepingId);
	}
	
	@Override
	public EntryGroup create(EntryGroup entryGroup) {
		return entryGroupService.create(entryGroup);
	}
	
	@Override
	public Optional<EntryGroup> findByEntryGroupId(UUID entryGroupId) {
		return entryGroupService.findByEntryGroupId(entryGroupId);
	}
	
	@Override
	public List<EntryGroup> findByBookkeepingId(UUID bookkeepingId) {
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
	public void deleteAllBybookkeepingId(UUID bookkeepingId) {
		entryGroupService.deleteAllBybookkeepingId(bookkeepingId);
	}
}
