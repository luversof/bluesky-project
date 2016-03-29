package net.luversof.bookkeeping.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.repository.EntryRepository;

@Service
@Transactional("bookkeepingTransactionManager")
public class EntryService {
	
	@Autowired
	private EntryRepository entryRepository;

	public Entry save(Entry entry) {
		return entryRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public Entry findOne(long id) {
		return entryRepository.findOne(id);
	}

	public void delete(Entry entry) {
		entryRepository.delete(entry);
	}
	
	@Transactional(value = "bookkeepingTransactionManager", readOnly = true)
	public List<Entry> findByBookkeepingId(long bookkeepingId) {
		return entryRepository.findByBookkeepingId(bookkeepingId);
	}
}
