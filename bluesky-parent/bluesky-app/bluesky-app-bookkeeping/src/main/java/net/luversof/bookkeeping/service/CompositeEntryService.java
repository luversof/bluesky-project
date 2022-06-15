package net.luversof.bookkeeping.service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.constant.EntryGroupType;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.util.BookkeepingUtils;

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
