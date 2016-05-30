package net.luversof.bookkeeping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntrySearchInfo;

@Service
public class EntrySearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public EntrySearchInfo getEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(entrySearchInfo.getBookkeeping().getId());
		entrySearchInfo.setBookkeeping(targetBookkeeping);
		entrySearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return entrySearchInfo;
	}

}
