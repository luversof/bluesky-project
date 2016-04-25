package net.luversof.bookkeeping.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntrySearchInfo;

@Service
public class EntrySearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public EntrySearchInfo getEntrySearchInfo(long bookkeepingId, LocalDate targetLocalDate) {
		if (targetLocalDate == null) {
			targetLocalDate = LocalDate.now();
		}
		EntrySearchInfo entrySearchInfo = new EntrySearchInfo();
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(bookkeepingId);
		entrySearchInfo.setTargetLocalDate(targetLocalDate);
		entrySearchInfo.setBookkeepingId(targetBookkeeping.getId());
		entrySearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return entrySearchInfo;
	}

}
