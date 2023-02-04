package net.luversof.api.bookkeeping.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;

@Service
public class CompositeEntryService implements EntryService {

	@Autowired
	private BasicEntryService entryService;

	@Override
	public Entry create(Entry entry) {
		return entryService.create(entry);
	}

	@Override
	public List<Entry> search(EntryRequestParam entryRequestParam) {
		return entryService.search(entryRequestParam);
	}

	@Override
	public List<Entry> findByBookkeepingIdAndEntryDateBetween(String bookkeepingId, LocalDate startLocalDate, LocalDate endLocalDate) {
		return entryService.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startLocalDate, endLocalDate);
	}

	@Override
	public Entry update(Entry entry) {
		return entryService.update(entry);
	}

	@Override
	public void delete(Entry entry) {
		entryService.delete(entry);
	}

	@Override
	public void deleteByBookkeepingId(String bookkeepingId) {
		entryService.deleteByBookkeepingId(bookkeepingId);
	}

}
