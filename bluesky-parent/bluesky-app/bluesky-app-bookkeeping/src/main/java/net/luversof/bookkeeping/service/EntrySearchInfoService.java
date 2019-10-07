package net.luversof.bookkeeping.service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.core.exception.BlueskyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntrySearchInfo;

@Service
public class EntrySearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public EntrySearchInfo getEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(entrySearchInfo.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entrySearchInfo.setBookkeeping(targetBookkeeping);
		entrySearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return entrySearchInfo;
	}

}
