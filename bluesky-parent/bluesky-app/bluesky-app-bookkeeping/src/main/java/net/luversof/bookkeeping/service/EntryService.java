package net.luversof.bookkeeping.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.core.exception.BlueskyException;

@Service
@Transactional("bookkeepingTransactionManager")
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	public Entry create(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		return entryRepository.save(entry);
	}
	
	public Entry update(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		Entry targetEntry = findOne(entry.getId());
		if (targetEntry.getBookkeeping().getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWER_ENTRY");
		}
		return entryRepository.save(entry);
	}

	@Transactional(value = "bookkeepingTransactionManager", readOnly = true)
	public Entry findOne(long id) {
		return entryRepository.findOne(id);
	}

	public void delete(Entry entry) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entry.getBookkeeping().getId());
		if (targetBookkeeping.getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWNER_BOOKKEEPING");
		}
		Entry targetEntry = findOne(entry.getId());
		if (targetEntry.getBookkeeping().getUserId() != entry.getBookkeeping().getUserId()) {
			throw new BlueskyException("NOT_OWER_ENTRY");
		}
		entryRepository.delete(entry);
	}
	
	@Transactional(value = "bookkeepingTransactionManager", readOnly = true)
	public List<Entry> findByBookkeepingId(long bookkeepingId) {
		return entryRepository.findByBookkeepingId(bookkeepingId);
	}
}
