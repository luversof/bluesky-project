//package net.luversof.api.bookkeeping.service;
//
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import net.luversof.api.bookkeeping.base.domain.Entry;
//import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;
//
//@Service
//public class CompositeEntryService implements EntryService {
//
//	@Autowired
//	private BasicEntryService entryService;
//
//	@Override
//	public Entry create(Entry entry) {
//		return entryService.create(entry);
//	}
//
//	@Override
//	public List<Entry> search(EntryRequestParam entryRequestParam) {
//		return entryService.search(entryRequestParam);
//	}
//
//	@Override
//	public List<Entry> findByBookkeepingIdAndEntryDateBetween(UUID bookkeepingId, ZonedDateTime startDate, ZonedDateTime endDate) {
//		return entryService.findByBookkeepingIdAndEntryDateBetween(bookkeepingId, startDate, endDate);
//	}
//
//	@Override
//	public Entry update(Entry entry) {
//		return entryService.update(entry);
//	}
//
//	@Override
//	public void delete(Entry entry) {
//		entryService.delete(entry);
//	}
//
//	@Override
//	public void deleteByBookkeepingId(UUID bookkeepingId) {
//		entryService.deleteByBookkeepingId(bookkeepingId);
//	}
//
//}
