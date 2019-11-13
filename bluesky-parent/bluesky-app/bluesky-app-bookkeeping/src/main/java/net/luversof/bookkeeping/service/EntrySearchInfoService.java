package net.luversof.bookkeeping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.boot.exception.BlueskyException;

@Service
public class EntrySearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public EntrySearchInfo getEntrySearchInfo(EntrySearchInfo entrySearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(entrySearchInfo.getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		entrySearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return entrySearchInfo;
	}

}
